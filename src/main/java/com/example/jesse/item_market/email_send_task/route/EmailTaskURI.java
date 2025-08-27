package com.example.jesse.item_market.email_send_task.route;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 邮件任务服务 URI 配置类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class EmailTaskURI
{
    /** 邮件任务根 URI */
    private static final String
    EMAIL_TASK_ROOT_URI = "/api/email-task";

    /** 启动 项目邮件任务 URI */
    public static final String
    START_EMAIL_TASK_URI = EMAIL_TASK_ROOT_URI + "/start";

    /** 停止 项目邮件任务 URI */
    public static final String
    STOP_EMAIL_TASK_URI = EMAIL_TASK_ROOT_URI + "/stop";

    /** 停止 延迟邮件任务有序集合的轮询 URI */
    public static final String
    STOP_POLL_DELAY_ZSET_URI = EMAIL_TASK_ROOT_URI + "/stop-poll-delay-zset";

    /** 停止 优先有序集合的邮件发送任务 URI */
    public static final String
    STOP_EMAIL_SENDER_TASK_URI = EMAIL_TASK_ROOT_URI + "/stop-email-send";

    /** 添加一个新的通用邮件任务 URI */
    public static final String
    ADD_EMAIL_TASK_URI = EMAIL_TASK_ROOT_URI + "/add-email-task";

    /** 添加一个新的验证码邮件任务 URI */
    public static final String
    ADD_VERIFY_CODE_EMAIL_TASK_URI = EMAIL_TASK_ROOT_URI + "/add-verify-email-task";

    /** 添加一个新的纯文本邮件任务 URI */
    public static final String
    ADD_TEXT_EMAIL_TASK_URI = EMAIL_TASK_ROOT_URI + "/add-text-email-task";

    public static final String
    ADD_ATTACHMENT_EMAIL_TASK_URI = EMAIL_TASK_ROOT_URI + "/add-attachment-email-task";
}