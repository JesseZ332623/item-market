package com.example.jesse.item_market.lock;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/** Redis 分布式锁接口类。*/
public interface RedisLock
{
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
    <T> Mono<T>
    withLock(
        String lockName,
        long acquireTimeout, long lockTimeout,
        Function<String, Mono<T>> action
    );
}
