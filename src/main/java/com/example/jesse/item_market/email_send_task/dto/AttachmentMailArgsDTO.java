package com.example.jesse.item_market.email_send_task.dto;

import lombok.*;

/** 携带附件的邮件构造参数 DTO。*/
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AttachmentMailArgsDTO
{
    /** 收件人姓名 */
    public String userName;

    /** 收件人邮箱 */
    public String userEmail;

    /** 邮件标题（按 “用户：XXX” 开头） */
    public String subject;

    /** 邮件正文 */
    public String message;

    /** 附件文件名 */
    String attachmentName;

    /** 附件完整数据 */
    byte[] attachmentData;
}
