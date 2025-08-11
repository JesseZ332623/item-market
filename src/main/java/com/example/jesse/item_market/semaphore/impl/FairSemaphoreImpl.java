package com.example.jesse.item_market.semaphore.impl;

import com.example.jesse.item_market.semaphore.FairSemaphore;
import com.example.jesse.item_market.semaphore.exception.AcquireSemaphoreFailed;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.utils.LuaScriptOperatorType.SEMAPHORE_OPERATOR;

/** Redis 公平信号量实现。*/
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
                        switch (result.getResult()) {
                            case "ACQUIRE_SEMAPHORE_FAILED" ->
                                Mono.error(
                                    new AcquireSemaphoreFailed(
                                        "Acquire semaphore failed! " +
                                        "Caused by: The resource is busy.",
                                        null
                                    )
                                );

                            case "SUCCESS" ->
                                Mono.just(identifier);

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
                        switch (result.getResult()) {
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
     * 使用 Mono.usingWhen() 方法，在信号量实例（FairSemaphoreImpl）的作用域内，
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
                action,
                (indentifier) ->
                    this.releaseFairSemaphore(semaphoreName, indentifier)
            )
        );
    }
}
