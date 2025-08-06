package com.example.jesse.item_market.guild.utils;

/** 当为一个前缀构建前驱与后继时失败所抛出的异常。*/
public class CreatePrefixRangeFailed extends RuntimeException
{
    public CreatePrefixRangeFailed(String message) {
        super(message);
    }

    public CreatePrefixRangeFailed(String message, Throwable throwable) {
      super(message, throwable);
  }
}
