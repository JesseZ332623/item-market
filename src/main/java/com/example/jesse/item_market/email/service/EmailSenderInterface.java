package com.example.jesse.item_market.email.service;

import com.example.jesse.item_market.email.dto.EmailContent;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/** 封装了 javax.mail 库的响应式邮件发送器接口。*/
public interface EmailSenderInterface
{
    /**
     * 外部可调用的发送邮件的方法。
     *
     * @param emailContent 邮件内容
     *
     * @return 表示操作是否正确完成的响应式流
     */
    Mono<Void>
    sendEmail(@NotNull EmailContent emailContent);
}
