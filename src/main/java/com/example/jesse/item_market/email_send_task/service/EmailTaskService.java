package com.example.jesse.item_market.email_send_task.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 邮件任务服务接口。*/
public interface EmailTaskService
{
    /** 启动 项目邮件任务。*/
    Mono<ServerResponse>
    startEmailTask(ServerRequest request);

    /** 停止 项目邮件任务。*/
    Mono<ServerResponse>
    stopEmailTask(ServerRequest request);

    /** 停止 延迟邮件任务有序集合的轮询。*/
    Mono<ServerResponse>
    stopPollDelayZset(ServerRequest request);

    /** 停止 优先有序集合的邮件发送任务。*/
    Mono<ServerResponse>
    stopExecuteEmailSenderTask(ServerRequest request);

    /** 添加一个新的验证码邮件任务。*/
    Mono<ServerResponse>
    addVerifyCodeEmailTask(ServerRequest request);

    /** 添加一个新的纯文本邮件任务。*/
    Mono<ServerResponse>
    addTextEmailTask(ServerRequest request);

    /** 添加一个新的携带附件的邮件任务。*/
    Mono<ServerResponse>
    addAttechmentEmailTask(ServerRequest request);

    /** 添加一个新的邮件任务。（通用添加操作）*/
    Mono<ServerResponse>
    addEmailTask(ServerRequest request);
}