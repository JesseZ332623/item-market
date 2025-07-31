package com.example.jesse.item_market.market.exception;

/** 当查询到卖家的武器不在市场上时，抛出本异常。*/
public class ItemNoOnMarket extends RuntimeException
{
    public ItemNoOnMarket(String message, Throwable throwable) {
        super(message, throwable);
    }
}
