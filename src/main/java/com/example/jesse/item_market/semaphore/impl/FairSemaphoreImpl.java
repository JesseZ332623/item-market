package com.example.jesse.item_market.semaphore.impl;

import com.example.jesse.item_market.semaphore.FairSemaphore;
import com.example.jesse.item_market.semaphore.exception.AcquireSemaphoreFailed;
import com.example.jesse.item_market.semaphore.exception.SemaphoreNotFound;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.utils.LuaScriptOperatorType.SEMAPHORE_OPERATOR;
import static java.lang.String.format;

/**
 * <p>Redis 公平信号量实现。</p>
 *
 * <i>
 *  需要强调的是，
 *  公平信号量（也可以视为分布式互斥锁）
 *  是限制使用同一个 Redis 服务的不同客户端访问某个资源的并发量的，
 *  不要和传统的互斥锁搞混了，也最好不要拿传统互斥锁的思维去使用它，
 *  否则多少有点大材小用了（笑）。
 * </i></br>
 *
 * <strong>
 *     对于同一线程内的多进程同步，应该使用：</br>
 *     {@link java.util.concurrent.Semaphore}
 * </strong>
 */
@Slf4j
@Component
public class FairSemaphoreImpl implements FairSemaphore
{
    /** Lua 脚本读取器。*/
    @Autowired
    private LuaScriptReader luaScriptReader;

    /** 执行 Lua 脚本专用 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, LuaOperatorResult> scriptRedisTemplate;

    /** 组合信号量拥有者有序集合键。*/
    @Contract(pure = true)
    private @NotNull String
    getSemaphoneOwnerKey(String semaphoreName) {
        return semaphoreName + ":" + "owner";
    }

    /** 组合信号量计数器数据键。*/
    @Contract(pure = true)
    private @NotNull String
    getSemaphoneCounterKey(String semaphoreName) {
        return semaphoreName + ":" + "counter";
    }

    /**
     * 进程尝试获取一个信号量。
     *
     * @param semaphoreName 信号量键名
     * @param limit         最大信号量值
     * @param timeout       信号量有效期
     *
     * @return 发布信号量唯一标识符的 Mono
     */
    private @NotNull Mono<String>
    acquireFairSemaphore(
        String semaphoreName, long limit, long timeout)
    {
        final String semaphoneOwnerKey
            = this.getSemaphoneOwnerKey(semaphoreName);

        final String semaphoneCountererKey
            = this.getSemaphoneCounterKey(semaphoreName);

        final String identifier
            = UUID.randomUUID().toString();

        return
        this.luaScriptReader
            .fromFile(SEMAPHORE_OPERATOR, "acquireFairSemaphore.lua")
            .flatMap((script) ->
                this.scriptRedisTemplate
                    .execute(
                        script,
                        List.of(semaphoreName, semaphoneOwnerKey, semaphoneCountererKey),
                        limit, timeout, identifier)
                    .timeout(Duration.ofSeconds(5L))
                    .next()
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "ACQUIRE_SEMAPHORE_FAILED" ->
                                Mono.error(
                                    new AcquireSemaphoreFailed(
                                        "Acquire semaphore failed! " +
                                        "Caused by: The resource is busy.",
                                        null
                                    )
                                );

                            case "SUCCESS" -> Mono.just(identifier);

                            case null, default ->
                                throw new IllegalStateException(
                                    "Unexpected value: " + result.getResult()
                                );
                        }
                    )
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null));
    }

    /**
     * 进程为了长期持有信号量，需要定期的对信号量进行刷新。
     *
     * @param semaphoreName 信号量键名
     * @param identifier    信号量唯一标识符
     *
     * @return 不发布任何数据的 Mono，表示操作是否完成
     */
    private @NotNull Mono<Void>
    refreshFairSemaphore(String semaphoreName, String identifier)
    {
        return
        this.luaScriptReader
            .fromFile(SEMAPHORE_OPERATOR,"refreshFairSemaphore.lua")
            .flatMap((script) ->
                this.scriptRedisTemplate
                    .execute(script, List.of(semaphoreName), identifier)
                    .timeout(Duration.ofSeconds(3L))
                    .next()
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "SEMAPHORE_NOT_FOUND" ->
                                Mono.error(
                                    new SemaphoreNotFound(
                                        format(
                                            "Fair semapore %s not exist in %s",
                                            identifier, semaphoreName),
                                        null
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                throw new IllegalStateException(
                                    "Unexpected value: " + result.getResult()
                                );
                        }
                    )
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 进程尝试释放一个信号量。
     *
     * @param semaphoreName 信号量键名
     * @param identifier    信号量唯一标识符
     *
     * @return 不发布任何数据的 Mono，表示操作是否完成
     */
    private @NotNull Mono<Void>
    releaseFairSemaphore(String semaphoreName, String identifier)
    {
        final String semaphoneOwnerKey
            = this.getSemaphoneOwnerKey(semaphoreName);

        return
        this.luaScriptReader
            .fromFile(SEMAPHORE_OPERATOR, "releaseFairSemaphore.lua")
            .flatMap((script) ->
                this.scriptRedisTemplate
                    .execute(
                        script,
                        List.of(semaphoreName, semaphoneOwnerKey),
                        identifier)
                    .timeout(Duration.ofSeconds(3L))
                    .next()
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "SEMAPHORE_TIMEOUT" ->
                                Mono.error(
                                    new SemaphoreNotFound(
                                        format("Semaphore: %s timeout.", identifier),
                                        null
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                throw new IllegalStateException(
                                    "Unexpected value: " + result.getResult()
                                );
                        }
                    )
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 兼容响应式流的 Redis 公平信号量操作，
     * 使用 Mono.usingWhen() 方法，在业务逻辑（action）范围前后，
     * 自动完成信号量的获取与释放操作。
     *
     * @param <T> 在信号量作用域中业务逻辑返回的类型
     *
     * @param semaphoreName 信号量键名（例：semaphore:remote）
     * @param limit         最大信号量值
     * @param timeout       信号量有效期
     * @param action        业务逻辑
     *
     * @return 发布业务逻辑执行结果数据的 Mono
     */
    @Override
    public <T> Mono<T>
    withFairSemaphore(
        String semaphoreName,
        long limit, long timeout,
        Function<String, Mono<T>> action)
    {
        return
        Mono.defer(() ->
            Mono.usingWhen(
                this.acquireFairSemaphore(semaphoreName, limit, timeout)
                    .map((identifier) -> identifier),
                (identifier) -> {
                    Mono<T> actionMono = action.apply(identifier);

                    // 对持有信号量时间较长的进程，才提供刷新功能
                    if (timeout > 10)
                    {
                        // 刷新间隔为超时时间的一半
                        Duration refreshInterval
                            = Duration.ofSeconds(timeout / 2);

                        /*
                         * 这里出现了几个复杂的响应式流操作，需要做出说明：
                         *
                         * 1. delayUntil() 延迟调用这个操作的流，直到提供给这个操作的的流
                         *   （此处是在长时间业务执行完毕前不断刷新信号量的操作）执行完毕，
                         *    才允许 actionMono 的完成信号向下游传播。
                         *
                         * 2. Flux.interval() 每间隔一段时间，递增然后发布一个 Long 值
                         *
                         * 3. takeUntilOther() 有条件的 take 操作，
                         *    直到收到业务逻辑 actionMono 执行完毕的信号
                         *    即 actionMono.ignoreElement().then(Mono.empty())，
                         *    才停止 Flux.interval() 的发布（业务执行完毕，停止刷新信号量）。
                         *
                         * 4. concatMap() 将每一个 Flux.interval() 发布的值映射为
                         *    refreshFairSemaphore() 操作，并按顺序执行。
                         *
                         * 5. then() 我们不关心刷新的结果，只关系刷新是否成功完成
                         */
                        return
                        actionMono.delayUntil((value) ->
                            Flux.interval(refreshInterval)
                                .takeUntilOther(
                                    actionMono.ignoreElement()
                                              .then(Mono.empty()))
                                .concatMap((ignore) ->
                                    this.refreshFairSemaphore(semaphoreName, identifier))
                                .then()
                        );
                    }

                    return actionMono;
                },
                (indentifier) ->
                    this.releaseFairSemaphore(semaphoreName, indentifier)
            )
        );
    }
}