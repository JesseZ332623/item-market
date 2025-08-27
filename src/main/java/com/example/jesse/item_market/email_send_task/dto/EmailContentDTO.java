package com.example.jesse.item_market.email_send_task.dto;

import com.example.jesse.item_market.email.dto.EmailContent;
import jakarta.annotation.Nullable;
import lombok.*;

/** 通用发送邮件内容，用于前端的 JSON 序列化 / 反序列化。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class EmailContentDTO
{
    /** 发给谁 (如 PerterGriffen@gmail.com) */
    private String to;

    /** 邮件主题 */
    private String subject;

    /** 邮件正文 */
    private String textBody;

    /** 附件文件名（可以为 null 表示没有附件）*/
    @Nullable
    private String attachmentName;

    /** 附件数据（可以为 null 表示没有附件）*/
    @Nullable
    private byte[] attachmentData;

    /** 转换成 {@link EmailContent} 类型。*/
    public EmailContent toEmailContent()
    {
        return new
        EmailContent(to, subject, textBody, attachmentName, attachmentData);
    }
}