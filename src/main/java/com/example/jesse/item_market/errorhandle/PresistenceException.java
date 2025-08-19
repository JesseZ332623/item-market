package com.example.jesse.item_market.errorhandle;

/** 本项目 MySQL 持久化过程中抛出的任何异常，都会汇总成本异常然后抛出。*/
public class PresistenceException extends RuntimeException
{
    public PresistenceException(String message) {
        super(message);
    }

    public PresistenceException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
