package com.example.jesse.item_market.errorhandle;

/** 本项目所有操作相关的异常，都会 re-throw 成本异常，然后向上传递。*/
public class ProjectOperatorException extends RuntimeException
{
    public ProjectOperatorException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
