package com.example.jesse.item_market.email_send_task;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email_send_task.dto.TaskPriority;
import org.springframework.data.redis.serializer.SerializationException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/** 基于 Redis 的邮件任务执行器接口。*/
public interface EmailSenderTask
{
    /**
     * 添加一个邮件任务。
     *
     * @param content  邮件内容
     * @param priority 邮件任务优先级
     * @param delay    邮件是否延时发布（无延时就填 Duration.ZERO）
     *
     * @throws SerializationException 当序列化邮件任务 DTO 失败时抛出
     *
     * @return 发布新任务唯一标识符的 Mono
     */
    Mono<String>
    addEmailTask(EmailContent content, TaskPriority priority, Duration delay);

    /** 启动延迟任务有序集合轮询。*/
    void startPollDelayZset();

    /** 关闭延迟任务有序集合轮询。*/
    void stopPollDelayZset();

    /** 启动优先有序集合的邮件发送任务。*/
    void startExcuteEmailSenderTask();

    /** 关闭优先有序集合的邮件发送任务。*/
    void stopExcuteEmailSenderTask();
}