package com.example.jesse.item_market.email_send_task.dto;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email_send_task.impl.TaskPriority;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** 邮件任务的封装。*/
@Getter
@ToString
@JsonTypeInfo(
    use      = JsonTypeInfo.Id.CLASS,        // 使用完全限定类名作为类型标识
    include  = JsonTypeInfo.As.PROPERTY,     // 作为单独属性包含
    property = "@class"                      // 类型信息的属性名
)
/* 只要任务的唯一标识完全相同，它们便是同一个任务 */
@EqualsAndHashCode(of = {"taskIdentifier"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailTaskDTO
{
    /** 邮件任务唯一标识符（在调用 create() 时自动生成）*/
    private String taskIdentifier;

    /** 邮件任务优先级。*/
    private String priorityName;

    /** 邮件任务优先级分数。*/
    private double priorityScore;

    /** 邮件内容。*/
    private EmailContent content;

    /** 创建一个邮件任务。*/
    public static @NotNull Mono<EmailTaskDTO>
    create(@NotNull TaskPriority priority, EmailContent content)
    {
        return
        Mono.just(
            new EmailTaskDTO(
                UUID.randomUUID().toString(),
                priority.getPriorityName(),
                priority.getPriorityScore(),
                content
            )
        );
    }
}