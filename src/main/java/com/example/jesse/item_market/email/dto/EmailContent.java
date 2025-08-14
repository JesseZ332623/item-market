package com.example.jesse.item_market.email.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Duration;

import static java.lang.String.format;

/**
 * 向指定用户发送邮件的内容实体。
 *（支持 Jacson 的序列化与反序列化）
 */
@Data
@ToString
@JsonTypeInfo(
    use      = JsonTypeInfo.Id.CLASS,        // 使用完全限定类名作为类型标识
    include  = JsonTypeInfo.As.PROPERTY,     // 作为单独属性包含
    property = "@class"                      // 类型信息的属性名
)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailContent
{
    /** 发给谁 (如 PerterGriffen@gmail.com) */
    private String to;

    /** 邮箱主题 */
    private String subject;

    /** 邮件正文 */
    private String textBody;

    /** 附件路径（可以为 null 表示没有附件）*/
    @Nullable
    private String attachmentPath;

    /**
     * 发送验证码邮件需要的内容。
     *
     * @param userName   收件人姓名
     * @param userEmail  收件人邮箱
     * @param varifyCode 验证码
     * @param expired    验证码有效期（一般从属性中获取）
     *
     * @return 发布验证码邮件内容的 Mono
     */
    public static @NotNull Mono<EmailContent>
    fromVarify(
        String userName, String userEmail,
        String varifyCode, @NotNull Duration expired)
    {
        return
        Mono.fromCallable(() -> {
            EmailContent emailContent = new EmailContent();

            emailContent.setTo(userEmail);
            emailContent.setSubject("用户：" + userName + " 请查收您的验证码。");
            emailContent.setTextBody(
                format(
                    "用户：%s 您的验证码是：[%s]，" +
                        "请在 %s 分钟内完成验证，超过 %s 分钟后验证码自动失效！",
                    userName, varifyCode,
                    expired.toMinutes(), expired.toMinutes()
                )
            );

            // 验证码邮件不需要附件内容
            emailContent.setAttachmentPath(null);

            return emailContent;
        });
    }

    /**
     * 发送带附件的邮件所需要的内容。
     *
     * @param userName   收件人姓名
     * @param userEmail  收件人邮箱
     * @param subject    邮件标题（按 “用户：XXX” 开头）
     * @param message    邮件正文
     * @param attachment 附件路径
     *
     * @return 发布带附件的邮件内容的 Mono
     */
    public static @NotNull Mono<EmailContent>
    formWithAttachment(
        String userName, String userEmail,
        String subject, String message, String attachment)
    {
        return
        Mono.fromCallable(() -> {
            EmailContent emailContent = new EmailContent();

            emailContent.setTo(userEmail);
            emailContent.setSubject("用户：" + userName + " " + subject);
            emailContent.setTextBody(message);

            Path attachmentPath
                = Path.of(attachment).normalize();

            emailContent.setAttachmentPath(attachmentPath.toString());

            return emailContent;
        });
    }
}
