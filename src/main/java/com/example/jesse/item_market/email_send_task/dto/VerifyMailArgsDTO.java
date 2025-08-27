package com.example.jesse.item_market.email_send_task.dto;

import lombok.*;

/** 验证码邮件构造参数 DTO。*/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access  = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class VerifyMailArgsDTO
{
    /** 收件人姓名 */
    private String userName;

    /** 收件人邮箱 */
    private String userEmail;

    /** 验证码有效期 */
    private Long expired;
}