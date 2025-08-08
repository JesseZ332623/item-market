package com.example.jesse.item_market.lock.impl;

import com.example.jesse.item_market.lock.RedisLock;
import com.example.jesse.item_market.lock.exception.RedisLockAcquireTimeout;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.utils.KeyConcat.getRedisLockKey;
import static com.example.jesse.item_market.utils.LuaScriptOperatorType.ACQUIRE_OPERATOR;
import static java.lang.String.format;

/** Redis 分布式锁实现类。*/
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class RedisLockImpl implements RedisLock
{
    /** Lua 脚本读取器。*/
    @Autowired
    private LuaScriptReader scriptReader;

    /** 执行 Lua 脚本专用 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, LuaOperatorResult> redisScriptTemplate;

    /** Redis Lock 实例的唯一标识符，外部不可访问。*/
    private String identifier = null;

    /** Redis Lock 实例的锁名，外部不可访问。*/
    private String lockName = null;

    /**
     * 尝试获取一个锁。
     *
     * @param lockName          锁名
     * @param acquireTimeout    获取锁的时间期限
     * @param lockTimeout       锁本身的有效期
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    private @NotNull Mono<Void>
    acquireLockTimeout(
        String lockName,
        long acquireTimeout, long lockTimeout)
    {
        final String identifier
            = UUID.randomUUID().toString();

        final String lockKeyName
            = getRedisLockKey(lockName);

        return
        this.scriptReader
            .fromFile(ACQUIRE_OPERATOR, "acquireLockTimeout.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(lockKeyName),
                        identifier, acquireTimeout, lockTimeout)
                    .next()
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "GET_LOCK_TIMEOUT" ->
                                Mono.error(
                                    new RedisLockAcquireTimeout(
                                        format(
                                            "Acquire lock: %s timeout! (acquireTimeout = %d seconds)",
                                            lockName, acquireTimeout
                                        ), null
                                    )
                                );

                            case "SUCCESS" -> {
                                this.identifier = identifier;
                                this.lockName   = lockName;
                                yield Mono.empty();
                            }

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
     * 尝试释放一个锁。
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    private @NotNull Mono<Void>
    releaseLock()
    {
        return
        Mono.defer(() -> {
            /* 先获取锁，才能释放锁。*/
            Objects.requireNonNull(
                this.identifier, "Could not obtain lock!"
            );

            final String lockKeyName
                = getRedisLockKey(this.lockName);

            return
            this.scriptReader
                .fromFile(ACQUIRE_OPERATOR, "releaseLock.lua")
                .flatMap((script) ->
                    this.redisScriptTemplate
                        .execute(script, List.of(lockKeyName), this.identifier)
                        .next()
                        .flatMap((result) ->
                            switch (result.getResult())
                            {
                                case "CONCURRENT_DELETE" -> {
                                    log.warn("Concurrent delete happend!");
                                    yield Mono.empty();
                                }

                                case "LOCK_OWNED_BY_OTHERS" -> {
                                    log.error("Try to delete others lock!");
                                    yield Mono.empty();
                                }

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
        });
    }

    /**
     * 兼容响应式流的 Redis 分布式锁操作，
     * 使用 Mono.usingWhen() 方法，在锁实例（RedisLockImpl）的作用域内，
     * 自动完成锁的获取与释放操作。
     *
     * @param <T> 在锁作用域中业务逻辑返回的类型
     *
     * @param lockName       锁名
     * @param acquireTimeout 获取锁的实现期限
     * @param lockTimeout    锁本身的持有时间期限
     * @param action         业务逻辑
     *
     * @return 发布业务逻辑执行结果数据的 Mono
     */
    @Override
    public <T> Mono<T>
    withLock(
        String lockName,
        long acquireTimeout, long lockTimeout,
        Function<RedisLockImpl, Mono<T>> action)
    {
        /* 注意外部再用 defer() 包一层，确保每次调用都创建新的响应式流。*/
        return
        Mono.defer(() -> {
            /*
             * 本方法中 Mono.usingWhen() 的工作流程是这样的：
             *
             * 1. 调用 this.acquireLockTimeout() ，
             *    成功后返回整个锁实例传递至管道下游
             *
             * 2. 上锁后执行业务逻辑
             *
             * 3. 业务逻辑执行解释后调用 releaseLock() 释放锁（不论成败）
             *
             * P.S. 即便业务逻辑执行过程中抛出异常，也能保证锁的释放
             */
            return
            Mono.usingWhen(
                this.acquireLockTimeout(lockName, acquireTimeout, lockTimeout)
                    .thenReturn(this),
                action,
                RedisLockImpl::releaseLock
            );
        });
    }
}