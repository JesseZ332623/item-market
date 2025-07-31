package com.example.jesse.item_market.market.exception;

/** 在用户尝试左手倒右手时抛出本异常。*/
public class SelfTransactional extends RuntimeException
{
  public SelfTransactional(String message, Throwable throwable) {
      super(message, throwable);
  }
}
