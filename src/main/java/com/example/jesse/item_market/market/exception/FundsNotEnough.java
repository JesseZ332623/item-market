package com.example.jesse.item_market.market.exception;

/** 当买家手里的钱买不起指定装备时抛出本异常。*/
public class FundsNotEnough extends RuntimeException
{
    public FundsNotEnough(String message, Throwable throwable) {
        super(message, throwable);
    }
}
