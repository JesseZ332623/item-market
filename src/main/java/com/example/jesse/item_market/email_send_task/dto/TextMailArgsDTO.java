package com.example.jesse.item_market.email_send_task.dto;

import lombok.*;

/** 纯文本邮件构造参数 DTO。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access  = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class TextMailArgsDTO
{
    /** 收件人邮箱 */
    private String userEmail;

    /** 邮件标题 */
    private String subject;

    /** 邮件正文 */
    private String message;
}
