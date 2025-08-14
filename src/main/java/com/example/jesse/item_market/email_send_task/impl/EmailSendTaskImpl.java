package com.example.jesse.item_market.email_send_task.impl;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email.exception.EmailException;
import com.example.jesse.item_market.email.service.EmailSenderInterface;
import com.example.jesse.item_market.email_send_task.EmailSenderTask;
import com.example.jesse.item_market.email_send_task.dto.EmailTaskDTO;
import com.example.jesse.item_market.email_send_task.dto.TaskPriority;
import com.example.jesse.item_market.email_send_task.dto.TaskType;
import com.example.jesse.item_market.lock.RedisLock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.jesse.item_market.email_send_task.dto.TaskType.*;
import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

/** 基于 Redis 的邮件任务执行器实现。*/
@Slf4j
@Service
public class EmailSendTaskImpl implements EmailSenderTask
{
    /** 通用响应式 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 响应式邮件发送器 */
    @Autowired
    private EmailSenderInterface emailSender;

    /** Redis 分布式锁。*/
    @Autowired
    private RedisLock redisLock;

    /**
     * 供本执行器使用的专用线程池，参数如下：。
     *
     * <ul>
     *     <li>池中工作线程数量：4 线程</li>
     *     <li>任务队列容量：    32 任务</li>
     *     <li>工作线程名前缀：  email-task</li>
     *     <li>空闲线程存活时间：60 秒</li>
     *     <li>开启守护线程</li>
     * </ul>
     */
    private final Scheduler emailTaskScheduler
        = Schedulers.newBoundedElastic(
            4, 32, "email-task",
        60, true
    );

    /** Jackson 序列化 / 反序列化器。*/
    private final ObjectMapper mapper = new ObjectMapper();

    /** 延迟任务有序集合轮询 启动/关闭 标志位。*/
    private static final AtomicBoolean POLL_DELAY_ZSET_STOP
        = new AtomicBoolean(true);

    /** 按优先级执行邮件发送任务的 启动/关闭 标志位。*/
    private static final AtomicBoolean EXCUTE_EMAIL_SEND_TASK_STOP
        = new AtomicBoolean(true);

    /** 在本类实例被销毁前，需要保证哪些执行的任务都停止。 */
    @PreDestroy
    void gracefulShoutDown()
    {
        this.stopExcuteEmailSenderTask();
        this.stopPollDelayZset();

        /* 此处我在想是否需要阻塞一下线程，确保残余任务执行完毕。*/
        try { Thread.sleep(Duration.ofSeconds(2L)); }
        catch (InterruptedException e) { throw new RuntimeException(e); }

        // 关闭本类专用的调度器，不影响全局
        this.emailTaskScheduler.dispose();
    }

    /**
     * 组合邮件任务键，有一下几种可能：
     *
     * <ol>
     *     <li>email-task:delay-task     延迟邮件任务</li>
     *     <li>email-task:priority-task  优先邮件任务</li>
     *     <li>email-task:death-task     死信队列</li>
     * </ol>
     *
     * @param taskType 邮件任务类型
     *
     * @return 邮件任务键
     */
    @Contract(pure = true)
    private static @NotNull String
    getEmailTaskKey(@NotNull TaskType taskType) {
        return "email-task:" + taskType.getType();
    }

    /**
     * 从 Redis 中获取当前时间戳，以小数的形式返回，格式如下所式：</br>
     *
     * <strong>1755066031.234070</strong>
     * <p>整数部分为秒，小数部分为微妙（即 1755066031 秒 + 0.234070 秒）</p>
     */
    private @NotNull Mono<Double>
    getRedisTimestamp()
    {
        return
        this.redisTemplate
            .getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .time(MICROSECONDS)
            .timeout(Duration.ofSeconds(3L))
            .map((time) -> {
                double stamp = (time.doubleValue() / (1000.00 * 1000.00));
                return Math.round(stamp * 1_000_000) / 1_000_000.0;
            })
            .cache(Duration.ofSeconds(1L))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null));
    }

    /** 启动延迟任务有序集合轮询。*/
    @Override
    public Mono<Void> startPollDelayZset()
    {
        POLL_DELAY_ZSET_STOP.set(false);

        // 显示调用 subscribeOn() 确保提交任务至专用的调度器。
        return
        this.pollDelayZset()
            .subscribeOn(this.emailTaskScheduler);
    }

    /** 关闭延迟任务有序集合轮询。*/
    @Override
    public void stopPollDelayZset() {
        POLL_DELAY_ZSET_STOP.set(true);
    }

    /** 启动优先有序集合的邮件发送任务。*/
    @Override
    public Mono<Void> startExcuteEmailSenderTask()
    {
        EXCUTE_EMAIL_SEND_TASK_STOP.set(false);

        return
        this.excuteEmailSenderTask()
            .subscribeOn(this.emailTaskScheduler);
    }

    /** 关闭优先有序集合的邮件发送任务。*/
    @Override
    public void stopExcuteEmailSenderTask() {
        EXCUTE_EMAIL_SEND_TASK_STOP.set(true);
    }

    /**
     * 添加一个邮件任务。
     *
     * @param content  邮件内容
     * @param priority 邮件任务优先级
     * @param delay    邮件是否延时发布（无延时就填 Duration.ZERO）
     *
     * @throws SerializationException 当序列化邮件任务 DTO 失败时抛出
     *
     * @return 发布新任务唯一标识符的 Mono
     */
    @Override
    public Mono<String>
    addEmailTask(
        EmailContent content,
        TaskPriority priority, Duration delay)
    {
        return
        EmailTaskDTO.create(priority, content)
            .flatMap((task) -> {
                final String emailTaskKey;
                final String taskJson;

                try {
                    taskJson = this.mapper.writeValueAsString(task);
                }
                catch (JsonProcessingException exception)
                {
                    throw new SerializationException(
                        format(
                            "Serialization instance of EmailTaskDTO failed! Caused by: %s",
                            exception.getMessage()),
                        exception
                    );
                }

                /*
                 * 对于没有延迟的邮件任务，
                 * 直接添加到优先级任务有序集合中去
                */
                if (delay.equals(Duration.ZERO))
                {
                    emailTaskKey = getEmailTaskKey(PRIORITY_TASK);

                    double priorityScore = priority.getPriorityScore();

                    return
                    this.redisTemplate
                        .opsForZSet()
                        .add(emailTaskKey, taskJson, priorityScore)
                        .then(Mono.just(task.getTaskIdentifier()));
                }
                else
                {
                    /*
                     * 反之对于有延迟执行的任务，需要先添加到延迟任务有序集合。
                     * 后面会有一个工作线程去执行定时任务，
                     * 将到达指定时间的任务放入优先级任务有序集合中去
                     */
                    emailTaskKey = getEmailTaskKey(DELAY_TASK);

                    return
                    this.getRedisTimestamp()
                        .map((timestamp) -> timestamp + delay.getSeconds())
                        .flatMap((delayTime) ->
                            this.redisTemplate
                                .opsForZSet()
                                .add(emailTaskKey, taskJson, delayTime)
                                .then(Mono.just(task.getTaskIdentifier()))
                        );
                }
            })
            .timeout(Duration.ofSeconds(5L))
            .onErrorResume(
                (exception) ->
                    redisGenericErrorHandel(exception, null));
    }

    /**
     * 轮询优先有序集合，从中取出优先级最高的邮件任务（在 ZSET 中其实就是倒数第一个），
     * 将它发送出去然后在集合中删除这个任务。</br>
     *
     * <i>（这个任务会在后台执行，前端可以随时 开启/暂停）</i>
     */
    private @NotNull Mono<Void>
    excuteEmailSenderTask()
    {
        final String prioritTaskyZsetKey = getEmailTaskKey(PRIORITY_TASK);

        return
        this.redisTemplate
            .opsForZSet()
            .reverseRange(prioritTaskyZsetKey, Range.open(0L, 0L))
            .timeout(Duration.ofSeconds(5L))
            .next()
            .switchIfEmpty(        // 若本次轮询发现集合中空无一物，延迟一小段时间以避免忙等待
                Mono.delay(Duration.ofSeconds(1L)).then(Mono.empty()))
            .map((task) -> (String) task)
            .flatMap(this::executeEmailSendAndRemove)
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .repeat(() -> !EXCUTE_EMAIL_SEND_TASK_STOP.get())
            .then();
    }

    /**
     * 轮询延迟任务有序集合，
     * 对于那些已经到达或超过所设延迟时间的邮件任务，
     * 把它取出来放到优先级有序集合中去。</br>
     *
     * <i>（这个任务会在后台执行，前端可以随时 开启/暂停）</i>
     */
    private @NotNull Mono<Void> pollDelayZset()
    {
        final String emailTaskKey = getEmailTaskKey(DELAY_TASK);

        return
        this.getRedisTimestamp()
            .flatMap((currenStamp) ->
                this.redisTemplate
                    .opsForZSet()
                    /* 通过 score 搜索 ZSET 中所有到期的邮件任务（ZRANGEBYSCORE key 0.00 currenStamp LIMIT 0 -1） */
                    .rangeByScore(emailTaskKey, Range.open(0.00, currenStamp), Limit.unlimited())
                    .timeout(Duration.ofSeconds(5L))
                    /* 做一下缓冲分批处理 */
                    .buffer(50)
                    .next()
                    .switchIfEmpty(
                        Mono.delay(Duration.ofSeconds(3L)).then(Mono.empty()))
                    .map((tasks) ->
                        tasks.stream().map((String::valueOf)).toList())
                    .flatMap((tasks) ->
                        Flux.fromIterable(tasks)
                            .flatMap(this::moveToPriorityZSet)
                            .then()
                    )
                    .onErrorResume((exception) ->
                        redisGenericErrorHandel(exception, null)))
            .repeat(() -> !POLL_DELAY_ZSET_STOP.get())
            .then();
    }

    /**
     * 当 pollDelayZset() 检查到
     * 延迟任务有序集合中的某个邮件发送任务到达所设的的时间，
     * 将它从延迟任务有序集合中移除，放入优先有序集合中去。</br>
     *
     * <p>一个完整的 EmailTask JSON 如下所式：</p>
     *
     * <pre><code>
     * {
     *   "@class" : "com.example.jesse.item_market.email_send_task.dto.EmailTaskDTO",
     *   "taskIdentifier" : "15029c1f-a66b-43cb-977d-0834f1ec9d9d",
     *   "priorityName" : "high",
     *   "priorityScore" : 90.0,
     *   "content" : {
     *     "@class" : "com.example.jesse.item_market.email.dto.EmailContent",
     *     "to" : "Peter-Griffin233@gmail.com",
     *     "subject" : "用户：Peter-Griffin 请查收您的验证码。",
     *     "textBody" : "用户：Peter-Griffin 您的验证码是：[10351615]，请在 5 分钟内完成验证，超过 5 分钟后验证码自动失效！",
     *     "attachmentPath" : null
     *   }
     * }
     * </code></pre>
     *
     * @param emailTaskJson 由 {@link EmailTaskDTO} 映射而来的 JSON 字符串，
     *                      表示一个完整的邮箱任务
     *
     * @return 不发布任何数据的 Mono，仅表示操作是否成功完成
     */
    private @NotNull Mono<Void>
    moveToPriorityZSet(String emailTaskJson)
    {
        return
        Mono.defer(() -> {
            final String delayTaskZsetKey    = getEmailTaskKey(DELAY_TASK);
            final String prioritTaskyZsetKey = getEmailTaskKey(PRIORITY_TASK);
            EmailTaskDTO emailTask;

            try
            {
                emailTask = this.mapper
                                .readValue(emailTaskJson, EmailTaskDTO.class);
            }
            catch (JsonProcessingException exception)
            {
                return Mono.error(
                    new SerializationException(
                        format(
                            "Deserialization instance of EmailTaskDTO failed! Caused by: %s",
                            exception.getMessage()
                        )
                    )
                );
            }

            return
            this.redisLock
                .withLock(
                    "MoveToPriorityZSet_Lock" + "-" + emailTask.getTaskIdentifier(),
                    5L, 5L,
                    (ignore) -> {
                        /* 从延迟邮件任务有序集合中移除 */
                        Mono<Void> removeFromDelayTaskZset
                            = this.redisTemplate
                                  .opsForZSet()
                                  .remove(delayTaskZsetKey, prioritTaskyZsetKey)
                                  .then();

                        /* 添加到优先邮件任务有序集合中去 */
                        Mono<Void> addToPriorityTaskZset
                            = this.redisTemplate
                                  .opsForZSet()
                                  .add(prioritTaskyZsetKey, emailTask, emailTask.getPriorityScore())
                                  .then();

                        /* 上述两个操作之间无依赖关系，调用 Mono.when() 并行执行。*/
                        return
                        Mono.when(removeFromDelayTaskZset, addToPriorityTaskZset)
                            .timeout(Duration.ofSeconds(5L));
                    }
                );
        })
        .onErrorResume((exception) ->
            redisGenericErrorHandel(exception, null));
    }

    /**
     * excuteEmailSenderTask() 读取优先有序集合中最要紧的邮件任务，
     * 然后交由本方法执行发送邮件的任务，
     * 发送完毕后要从删除优先有序集合中删除这个任务。</br>
     *
     * <p>一个完整的 EmailTask JSON 如下所式：</p>
     *
     * <pre><code>
     * {
     *   "@class" : "com.example.jesse.item_market.email_send_task.dto.EmailTaskDTO",
     *   "taskIdentifier" : "15029c1f-a66b-43cb-977d-0834f1ec9d9d",
     *   "priorityName" : "high",
     *   "priorityScore" : 90.0,
     *   "content" : {
     *     "@class" : "com.example.jesse.item_market.email.dto.EmailContent",
     *     "to" : "Peter-Griffin233@gmail.com",
     *     "subject" : "用户：Peter-Griffin 请查收您的验证码。",
     *     "textBody" : "用户：Peter-Griffin 您的验证码是：[10351615]，请在 5 分钟内完成验证，超过 5 分钟后验证码自动失效！",
     *     "attachmentPath" : null
     *   }
     * }
     * </code></pre>
     *
     * @param emailTaskJson 由 {@link EmailTaskDTO} 映射而来的 JSON 字符串，
     *                      表示一个完整的邮箱任务
     *
     * @return 不发布任何数据的 Mono，仅表示操作是否成功完成
     */
    private @NotNull Mono<Void>
    executeEmailSendAndRemove(String emailTaskJson)
    {
        return
        Mono.defer(() -> {
            final String prioritTaskyZsetKey = getEmailTaskKey(PRIORITY_TASK);
            EmailTaskDTO emailTask;

            try
            {
                emailTask = this.mapper
                                .readValue(emailTaskJson, EmailTaskDTO.class);
            }
            catch (JsonProcessingException exception)
            {
                return Mono.error(
                    new SerializationException(
                        format(
                            "Deserialization instance of EmailTaskDTO failed! Caused by: %s",
                            exception.getMessage()
                        )
                    )
                );
            }

            return
            this.redisLock
                .withLock(
                    "ExcuteEmailSendTaskAndRemove_Lock" + "-" + emailTask.getTaskIdentifier(),
                    5L, 15L,
                    (ignore) -> {

                        /* 正式的发送邮件。 */
                        Mono<Void> sendEmail
                            = this.emailSender
                                  .sendEmail(emailTask.getContent())
                                 /* 对于多次尝试发送仍旧失败的邮件，放入死信队列。*/
                                  .onErrorResume(
                                      EmailException.class,
                                      (exception) ->
                                          this.moveToDeadMailQueue(emailTaskJson)
                                  );

                        /* 将这个任务从优先有序集合中移除。*/
                        Mono<Void> removeFromPriorityZSet
                            = this.redisTemplate
                                  .opsForZSet()
                                  .remove(prioritTaskyZsetKey, emailTaskJson)
                                  .then();

                        /*
                         * 虽然上述两个操作之间无依赖关系，
                         * 但是邮件发送操作失败概率很高，所以必须让 sendEmail 执行成功，
                         * 再订阅 removeFromPriorityZSet 操作，避免任务丢失。
                         */
                        return
                        sendEmail.then(removeFromPriorityZSet)
                                 .timeout(Duration.ofSeconds(15L));
                    });
        })
        .onErrorResume((exception) ->
            redisGenericErrorHandel(exception, null));
    }

    /**
     * 对于哪些多次尝试发送但仍旧失败的邮件任务，移入死信队列。
     *
     * @param emailTaskJson 死信任务 JSON 字符串
     *
     * @return 不发布任何数据的 Mono，仅表示操作是否成功完成
     */
    private @NotNull Mono<Void>
    moveToDeadMailQueue(String emailTaskJson)
    {
        final String deadthQueueKey = getEmailTaskKey(DEATH_TASK);

        return
        this.redisTemplate
            .opsForList()
            .leftPush(deadthQueueKey, emailTaskJson)
            .then();
    }
}