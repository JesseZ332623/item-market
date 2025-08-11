package com.example.jesse.item_market.semaphore.exception;

/** 当获取信号量失败时抛出本异常。*/
public class AcquireSemaphoreFailed extends RuntimeException
{
    public AcquireSemaphoreFailed(String message) { super(message); }

    public AcquireSemaphoreFailed(String message, Throwable throwable) {
        super(message, throwable);
    }
}
