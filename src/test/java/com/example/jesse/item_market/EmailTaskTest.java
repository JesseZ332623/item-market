package com.example.jesse.item_market;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email_send_task.dto.TaskPriority;
import com.example.jesse.item_market.email_send_task.impl.EmailSendTaskImpl;
import com.example.jesse.item_market.utils.UUIDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 邮件任务执行器测试类。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmailTaskTest
{
    /** 通用响应式 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailSendTaskImpl emailSendTask;

    private static final List<TaskPriority> TASK_PRIORITY_LIST
        = Arrays.asList(TaskPriority.values());

    /** 获取随机的优先级。*/
    private TaskPriority randomPriority()
    {
        return TASK_PRIORITY_LIST.get(
            ThreadLocalRandom.current()
                .nextInt(0, TASK_PRIORITY_LIST.size())
        );
    }

    /** 获取随机的延时（包括不延时）。*/
    private Duration
    randomDelay(long maxDelay)
    {
        return
            Duration.ofSeconds(
                ThreadLocalRandom.current()
                    .nextLong(0L, maxDelay)
            );
    }

    /** 随机时间间隔生成无限数量的邮件内容。*/
    private @NotNull Flux<EmailContent>
    generateInfEmailContent()
    {
        return
        Flux.interval(Duration.ofMillis(
            ThreadLocalRandom.current()
                             .nextLong(500L, 800L)))
            .flatMap((ignore) -> {

                String userId = UUIDGenerator.generateAsSting();

                return
                EmailContent.formWithAttachment(
                    userId, userId + "@gmail.com",
                    "An important message for you!",
                    "Best regards to user: " + userId,
                    "E:\\图片素材\\趴窗的普拉纳.jpg"
                );
            });
    }

    public Mono<Void> AddEmailTask()
    {
        return
        this.generateInfEmailContent()
            .buffer(5)
            .flatMap((contents) ->
                Flux.fromIterable(contents)
                    .flatMap((content) ->
                        this.emailSendTask
                            .addEmailTask(
                                content,
                                this.randomPriority(),
                                this.randomDelay(10L)
                    )
                )
            ).then();
    }

    /** 邮件任务执行器较高负载 1 分钟运行测试。（编译时快速通过）*/
    @Test
    @Order(1)
    public void TestPollAndExecute() throws InterruptedException
    {
        Mono.when(
            this.AddEmailTask(),
            this.emailSendTask.startPollDelayZset(),
            this.emailSendTask.startExcuteEmailSenderTask()
        ).subscribe();

        Thread.sleep(Duration.ofMinutes(1L));
    }

    /** 最后调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
    @Test
    @Order(2)
    public void redisFlushAllAsync()
    {
        this.redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll(RedisServerCommands.FlushOption.ASYNC)
            .doOnSuccess(System.out::println)
            .block();
    }
}

