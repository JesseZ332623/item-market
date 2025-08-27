package com.example.jesse.item_market.email_send_task.service.impl;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email.utils.VerifyCodeGenerator;
import com.example.jesse.item_market.email_send_task.EmailSenderTask;
import com.example.jesse.item_market.email_send_task.dto.AttachmentMailArgsDTO;
import com.example.jesse.item_market.email_send_task.dto.EmailContentDTO;
import com.example.jesse.item_market.email_send_task.dto.TextMailArgsDTO;
import com.example.jesse.item_market.email_send_task.dto.VerifyMailArgsDTO;
import com.example.jesse.item_market.email_send_task.impl.TaskPriority;
import com.example.jesse.item_market.email_send_task.service.EmailTaskService;
import com.example.jesse.item_market.response.ResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.example.jesse.item_market.response.URLParamPrase.praseNumberRequestParam;
import static com.example.jesse.item_market.response.URLParamPrase.praseRequestParam;
import static java.lang.String.format;

/** 邮件任务服务实现。*/
@Slf4j
@Service
public class EmailTaskServiceImpl implements EmailTaskService
{
    @Autowired
    private EmailSenderTask emailSenderTask;

    @Autowired
    private ResponseBuilder responseBuilder;

    /** 启动 项目邮件任务。*/
    @Override
    public Mono<ServerResponse>
    startEmailTask(ServerRequest request)
    {
        if (this.emailSenderTask.isTaskStarted())
        {
            return
            this.responseBuilder
                .BAD_REQUEST("Email task has been started!", null);
        }

        Mono.when(
            this.emailSenderTask
                .startPollDelayZset(),
            this.emailSenderTask
                .startExcuteEmailSenderTask())
        .subscribe();

        return
        this.responseBuilder
            .OK(
                null, "Email task started!",
                null, null
            );
    }

    /** 停止 项目邮件任务。*/
    @Override
    public Mono<ServerResponse>
    stopEmailTask(ServerRequest request)
    {
        return
        Mono.fromRunnable(() -> {
            this.emailSenderTask.stopPollDelayZset();
            this.emailSenderTask.stopExcuteEmailSenderTask();
        })
        .then(
            this.responseBuilder
                .OK(
                    null, "Email task stop!",
                    null, null))
        .onErrorResume((excption) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(
                    format(
                        "Stop email task failed! Caused by: %s", excption.getMessage()),
                    excption
                )
            );
    }

    /** 停止 延迟邮件任务有序集合的轮询。*/
    @Override
    public Mono<ServerResponse>
    stopPollDelayZset(ServerRequest request)
    {
        return
        Mono.fromRunnable(() ->
            this.emailSenderTask
                .stopPollDelayZset())
            .then(
                this.responseBuilder
                    .OK(
                        null, "Stop poll delay zset complete!",
                        null, null
                    ))
            .onErrorResume((excption) ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        format(
                            "Stop poll delay zset failed! Caused by: %s",
                            excption.getMessage()
                        ), excption
                    )
            );
    }

    /** 停止 优先有序集合的邮件发送任务。*/
    @Override
    public Mono<ServerResponse>
    stopExecuteEmailSenderTask(ServerRequest request)
    {
        return
        Mono.fromRunnable(() ->
                this.emailSenderTask
                    .stopPollDelayZset())
            .then(
                this.responseBuilder
                    .OK(
                        null, "Stop excute email sender complete!",
                        null, null
                    ))
            .onErrorResume((excption) ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        format(
                            "Stop excute email sender failed! Caused by: %s",
                            excption.getMessage()
                        ), excption
                    )
                );
    }

    /** 添加一个新的验证码邮件任务。*/
    @Override
    public Mono<ServerResponse>
    addVerifyCodeEmailTask(@NotNull ServerRequest request)
    {
        return
        Mono.zip(
            request.bodyToMono(VerifyMailArgsDTO.class),
            VerifyCodeGenerator.generateVerifyCode(6))
        .flatMap((mailParts) -> {
            final VerifyMailArgsDTO verifyEmailArgs = mailParts.getT1();
            final String            verifyCode      = mailParts.getT2();

            return
            EmailContent.fromVarify(
                verifyEmailArgs.getUserName(),
                verifyEmailArgs.getUserEmail(),
                verifyCode,
                Duration.ofMinutes(verifyEmailArgs.getExpired())
            );
        })
        .flatMap((mailContent) ->
            /* 验证码邮件 优先级：高，延迟发送：无 */
            this.emailSenderTask
                .addEmailTask(mailContent, TaskPriority.HIGH, Duration.ZERO)
        )
        .flatMap((taskId) ->
            this.responseBuilder
                .OK(
                    null,
                    format("Add verify code mail task complete! Task ID: %s", taskId),
                    null, null
                )
        )
        .onErrorResume((exception) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(
                    format(
                        "Add new verify code email task failed! Cause by: %s",
                        exception.getMessage()
                    ), exception
                )
        );
    }

    /** 添加一个新的纯文本邮件任务。*/
    @Override
    public Mono<ServerResponse>
    addTextEmailTask(@NotNull ServerRequest request)
    {
        return
        Mono.zip(
            request.bodyToMono(TextMailArgsDTO.class),
            praseRequestParam(request, "priority")
                .map((p) -> TaskPriority.valueOf(p.toUpperCase())),
            praseNumberRequestParam(request, "delay", Long::parseLong)
                .map(Duration::ofSeconds))
        .flatMap((mailParams) -> {
            final TextMailArgsDTO textMailArgs = mailParams.getT1();
            final TaskPriority    priority     = mailParams.getT2();
            final Duration        taskDelay    = mailParams.getT3();

            return
            EmailContent.fromJustText(
                textMailArgs.getUserEmail(),
                textMailArgs.getSubject(),
                textMailArgs.getMessage())
            .flatMap((mailContent) ->
                this.emailSenderTask
                    .addEmailTask(mailContent, priority, taskDelay)
            );
        })
        .flatMap((taskId) ->
            this.responseBuilder
                .OK(
                    null,
                    format("Add text mail task complete! Task ID: %s", taskId),
                    null, null
                )
        )
        .onErrorResume((exception) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(
                    format(
                        "Add new text email task failed! Cause by: %s",
                        exception.getMessage()
                    ), exception
                )
        );
    }

    /** 添加一个新的携带附件的邮件任务。*/
    @Override
    public Mono<ServerResponse>
    addAttechmentEmailTask(@NotNull ServerRequest request)
    {
        return
        Mono.zip(
            request.bodyToMono(AttachmentMailArgsDTO.class),
            praseRequestParam(request, "priority")
                .map((p) -> TaskPriority.valueOf(p.toUpperCase())),
            praseNumberRequestParam(request, "delay", Long::parseLong)
                .map(Duration::ofSeconds))
        .flatMap((mailParams) -> {
            final AttachmentMailArgsDTO
                attachmentMainArgs      = mailParams.getT1();
            final TaskPriority priority = mailParams.getT2();
            final Duration taskDelay    = mailParams.getT3();

            return
            EmailContent.formWithAttachment(
                attachmentMainArgs.getUserName(),
                attachmentMainArgs.getUserEmail(),
                attachmentMainArgs.getSubject(),
                attachmentMainArgs.getMessage(),
                attachmentMainArgs.getAttachmentName(),
                attachmentMainArgs.getAttachmentData())
            .flatMap((emailContent) ->
                this.emailSenderTask
                    .addEmailTask(emailContent, priority, taskDelay));
        })
        .flatMap((taskId) ->
            this.responseBuilder
                .OK(
                    null,
                    format("Add text mail task complete! Task ID: %s", taskId),
                    null, null
                )
        )
        .onErrorResume((exception) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(
                    format(
                        "Add new text email task failed! Cause by: %s",
                        exception.getMessage()
                    ), exception
                )
        );
    }

    /** 添加一个新的邮件任务。（通用添加操作）*/
    @Override
    public Mono<ServerResponse>
    addEmailTask(@NotNull ServerRequest request)
    {
        return
        Mono.zip(
            request.bodyToMono(EmailContentDTO.class),
            praseRequestParam(request, "priority")
                .map((p) -> TaskPriority.valueOf(p.toUpperCase())),
            praseNumberRequestParam(request, "delay", Long::parseLong)
                .map(Duration::ofSeconds))
        .flatMap((requestData) -> {
            final EmailContent emailContent = requestData.getT1().toEmailContent();
            final TaskPriority taskPriority = requestData.getT2();
            final Duration     delay        = requestData.getT3();

            return
            this.emailSenderTask
                .addEmailTask(emailContent, taskPriority, delay);
        })
        .flatMap((taskId) ->
            this.responseBuilder
                .OK(
                    null,
                    format("Create email task complete! Task ID: %s", taskId),
                    null, null
                ))
        .onErrorResume((exception) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(
                    format(
                        "Add new email task failed! Cause by: %s",
                        exception.getMessage()
                    ), exception
                )
        );
    }
}