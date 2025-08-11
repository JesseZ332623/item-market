package com.example.jesse.item_market.semaphore;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/** Redis 公平信号量接口。*/
public interface FairSemaphore
{
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
    <T> Mono<T>
    withFairSemaphore(
        String semaphoreName,
        long limit, long timeout,
        Function<String, Mono<T>> action
    );
}