package com.example.jesse.item_market.email.exception;

import lombok.Getter;

/** 邮件发送异常类。*/
@Getter
public class EmailException extends RuntimeException
{
    /**
     * 邮件发送过程中会出现很多原因导致的错误，
     * 具体分下面 4 类：
     */
    public enum ErrorType
    {
        /** 邮箱服务授权码错误。 */
        AUTH_FAILURE,

        /** 网络错误。 */
        NETWORK_ISSUE,

        /** 错误的邮箱地址格式。 */
        INVALID_CONTENT,

        /** 配置属性错误。 */
        CONFIG_MISSING,

        /** 附件不存在错误。*/
        ATTACHMENT_NOT_EXIST
    }

    private final ErrorType errorType;

    /**
     * 邮件发送异常的构造。
     *
     * @param errorType     错误类型
     * @param message       异常信息
     * @param cause         异常本体（必须是 RuntimeException 的子类型异常）
     */
    public EmailException(
        ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
}
