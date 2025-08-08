package com.example.jesse.item_market.lock.exception;

/** 当等待获取 Redis 锁超过设定的时间限制，抛本异常。*/
public class RedisLockAcquireTimeout extends RuntimeException
{
    public RedisLockAcquireTimeout(String message) {
        super(message);
    }
    public RedisLockAcquireTimeout(String message, Throwable throwable) {
        super(message, throwable);
    }
}
