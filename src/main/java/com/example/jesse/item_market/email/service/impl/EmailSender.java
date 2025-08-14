package com.example.jesse.item_market.email.service.impl;


import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email.service.EmailSenderInterface;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import com.example.jesse.item_market.email.exception.EmailException;
import com.example.jesse.item_market.email.utils.*;

import static com.example.jesse.item_market.email.authkey.EmailAuthRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.example.jesse.item_market.email.authkey.EmailAuthRedisKey.SERVICE_AUTH_CODE;
import static com.example.jesse.item_market.email.exception.EmailException.ErrorType.ATTACHMENT_NOT_EXIST;
import static com.example.jesse.item_market.email.exception.EmailException.ErrorType.INVALID_CONTENT;
import static java.lang.String.format;

/** 封装了 javax.mail 库的响应式邮件发送器。*/
@Data
@Slf4j
@Component
public class EmailSender implements EmailSenderInterface
{
    /** 提供邮箱服务的域名。*/
    private final static String SMTP_HOST = "smtp.qq.com";

    /** 邮箱服务端口。*/
    private final static int    SMTP_PORT = 465;

    /** 最大邮件发送尝试次数。*/
    private static int MAX_ATTEMPT_TIMES  = 3;

    /** 附件大小的上限：16 MB */
    private static int MAX_ATTACHMENT_SIZE = 16 * 1024 * 1024;

    /** 提供 SMTP 服务的运营商主机名 (例：smtp.gmail.com、smtp.qq.com)。*/
    private final String smtpHost;

    /**
     * SMTP 端口号，不同的服务商对邮件开放的标准服务端口都不同，具体如下所示：
     * <pre>
     *     Gmail   587 STARTTLS
     *     Outlook 587 STARTTLS
     *     Yahoo   465 SSL
     *     QQ-Mail 465 or 587 SSL
     * </pre>
     * 其他服务商的端口号可以查询他们提供的文档。
     */
    private final int smtpPort;

    /** 邮件配置属性 */
    private final Properties mailProperties;

    /** Redis 模板实例 */
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * 邮件发送器构造函数，在调用 EmailSenderBuilder::build() 时调用，
     * 外部不可以直接调用。
     *
     * @param builder 邮件发送器实例生成器
     */
    private EmailSender(
        @NotNull
        EmailSenderBuilder builder)
    {
        this.smtpHost            = builder.getSmtpHost();
        this.smtpPort            = builder.getSmtpPort();
        this.mailProperties      = builder.getMailProperties();
        this.redisTemplate       = builder.getRedisTemplate();
    }

    @Bean(name = "createEmailSender")
    public static EmailSender createEmailSender(
        ReactiveRedisTemplate<String, Object> redisTemplate
    )
    {
        return new EmailSender.EmailSenderBuilder()
            .smtpHost(SMTP_HOST)
            .smtpPort(SMTP_PORT)
            .redisTemplate(redisTemplate)
            .defaultSetProperties()
            .build();
    }

    /**
     * <p>邮件发送器实例生成器。</p>
     *
     * <span>
     *     在 createEmailSender() 工厂方法中用到了该生成器，
     *     因此需要 @Component 注解标记为一个组件，使得 Spring 能识别到它。
     * </span>
     */
    @Data
    @Component
    public static class EmailSenderBuilder
    {
        /** 提供 SMTP 服务的运营商主机名 */
        private String smtpHost             = null;

        /** SMTP 端口号 */
        private int    smtpPort             = -1;

        /** 邮件配置属性 */
        private Properties  mailProperties  = null;

        /** Redis 模板实例 */
        private ReactiveRedisTemplate<String, Object> redisTemplate = null;

        /** 设置提供 SMTP 服务的运营商主机名。*/
        public EmailSenderBuilder smtpHost(String host) {
            this.smtpHost = host; return this;
        }

        /** 设置 SMTP 端口号。*/
        public EmailSenderBuilder smtpPort(int port) {
            this.smtpPort = port; return this;
        }

        /** 注入 Redis 模板实例。 */
        public EmailSenderBuilder
        redisTemplate(ReactiveRedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate; return this;
        }

        /** 添加邮件服务配置属性（暂时用不到，但考虑扩展性予以保留）。*/
        public EmailSenderBuilder
        addProperty(String key, String value)
        {
            if (this.mailProperties == null) {
                this.mailProperties = new Properties();
            }

            mailProperties.put(key, value);

            return this;
        }

        /** 配置默认的邮件属性。*/
        public EmailSenderBuilder defaultSetProperties()
        {
            if (this.smtpPort != -1 || this.smtpHost != null)
            {
                this.mailProperties = new Properties();

                this.mailProperties.put("mail.smtp.auth", "true");
                this.mailProperties.put("mail.smtp.host", this.smtpHost);
                this.mailProperties.put("mail.smtp.port", this.smtpPort);
                this.mailProperties.put("mail.smtp.connectionpool", "true");
                this.mailProperties.put("mail.smtp.connectionpooltimeout", "5000");
                this.mailProperties.put("mail.smtp.connectionpoolsize", "10");

                switch (this.smtpPort)
                {
                    case 465:
                        this.mailProperties.put("mail.smtp.ssl.enable", "true");
                        break;

                    case 587:
                        this.mailProperties.put("mail.smtp.starttls.enable", "true");
                        break;
                }
            }
            else
            {
                throw new IllegalStateException(
                    "[IllegalStateException] Couldn't set Properties, " +
                        "Cause: Not setting SMTP Port or SMTP Host."
                );
            }

            return this;
        }

        /** 字段设置完毕，构造出实例并返回。*/
        public EmailSender build() {
            return new EmailSender(this);
        }
    }

    /**
     * 创建邮件发送会话的静态方法。
     *
     * @param props     邮件配置属性
     * @param userName  邮件发送人用户名
     * @param password  邮箱服务授权码
     *
     * @return 构造好的承载了一个 Session 实例的 Mono
     */
    @Contract("_, _, _ -> new")
    private static @NotNull Session createSession(
        Properties props,
        String userName, String password)
    {
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });
    }

    /**
     * 从 Redis 中查询发件人邮箱号和对应的服务授权码，
     * 返回一个承载了上述两个信息的元组。
     */
    private @NotNull Mono<Tuple2<String, String>>
    getEmailPublisherInfo()
    {
        return Mono.zip(
            this.redisTemplate.opsForValue()
                .get(ENTERPRISE_EMAIL_ADDRESS.toString())
                .map((address) -> (String) address),
            this.redisTemplate.opsForValue()
                .get(SERVICE_AUTH_CODE.toString())
                .map((authCode) -> (String) authCode)
        );
    }

    /**
     * 检查在发送邮件过程中所抛出的异常，
     * 是否有重发邮件的必要？
     */
    private boolean
    isRetryableError(Throwable throwable)
    {
        if (throwable instanceof EmailException exception) {
            // 只有因为网络问题导致的发生失败，才需要进行重试。
            return exception.getErrorType() == EmailException.ErrorType.NETWORK_ISSUE;
        }
        
        return false;
    }

    /** 构建邮件正文的数据。*/
    private static @NotNull Multipart
    getMultipart(@NotNull EmailContent content) throws MessagingException, IOException
    {
        Multipart multipart = new MimeMultipart();
        BodyPart  textPart  = new MimeBodyPart();

        textPart.setText(content.getTextBody());
        multipart.addBodyPart(textPart);

        // 若 content 内的附件路径不为空，则需要添加附件
        if (content.getAttachmentPath() != null)
        {
            File attachment = getAttachment(content.getAttachmentPath());

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(attachment);

            multipart.addBodyPart(attachmentPart);
        }

        return multipart;
    }

    /** 按照提供的附件路径构建附件。*/
    private static @NotNull File
    getAttachment(@NotNull String attachmentPath)
    {
        File attachment = Path.of(attachmentPath)
                              .normalize()
                              .toFile();

        // 检查附件路径是否存在
        if (!attachment.exists())
        {
            throw new EmailException(
                INVALID_CONTENT,
                format("Attachment path: %s not found!", attachmentPath),
                null
            );
        }

        // 检查附件的大小有没有超过最大值
        if (attachment.length() > MAX_ATTACHMENT_SIZE)
        {
            throw new EmailException(
                INVALID_CONTENT,
                "Attachment too large! (MAX_ATTACHMENT_SIZE = " +
                MAX_ATTACHMENT_SIZE / 1024 / 1024 + " MB)",
                null
            );
        }

        return attachment;
    }

    /**
     * <p>
     *     邮件发送的主要逻辑，由于传统的邮件发送是阻塞式的，
     *     所有我需要调用 Mono.fromCallable() 把整个邮件组装发送的逻辑封装，
     *     然后调用 subscribeOn(Schedulers.boundedElastic()) 将整个任务提交给线程池执行。
     * </p>
     *
     * <p>
     *     还需要注意的是函数式接口 Callable 是没有输入但有输出 T 的，
     *     但邮件发送逻辑没什么好返回的，所以方法最后调用 then() 发出一个 Mono{@literal <Void>}，
     *     表示返回的响应式流不承载任何数据。
     * </p>
     *
     * @param content 邮件内容
     * @param fromName 发件人
     * @param authCode 邮箱服务授权码
     *
     * @return 不承载任何数据的 Mono，表示操作成功完成
     */
    private @NotNull Mono<Void>
    sendEmailReactive(EmailContent content, String fromName, String authCode)
    {
        return Mono.fromCallable(() -> {
            try
            {
                Session newSession
                    = createSession(this.mailProperties, fromName, authCode);

                Message message
                    = new MimeMessage(newSession);

                if (fromName != null) {
                    message.setFrom(new InternetAddress(fromName));
                }

                message.setRecipient(
                    Message.RecipientType.TO,
                    new InternetAddress(content.getTo())
                );

                message.setSubject(content.getSubject());

                if (content.getAttachmentPath() == null) {
                    message.setText(content.getTextBody());
                }
                else {
                    message.setContent(getMultipart(content));
                }

                Transport.send(message);

                return null;
            }
            catch (AuthenticationFailedException exception)
            {
                throw new EmailException(
                    EmailException.ErrorType.AUTH_FAILURE,
                    "SMTP auth failed!", exception
                );
            }
            catch (IOException exception)
            {
                throw new EmailException(
                    ATTACHMENT_NOT_EXIST,
                    "Attachment error!", exception
                );
            }
            catch (MessagingException exception)
            {
                throw new EmailException(
                    EmailException.ErrorType.NETWORK_ISSUE,
                    "Net work issue!", exception
                );
            }

        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * 外部可调用的发送邮件的方法。
     *
     * @param emailContent 邮件内容
     *
     * @throws SerializationException 当序列化邮件任务 DTO 失败时抛出
     *
     * @return 表示操作是否正确完成的响应式流
     */
    @Override
    public Mono<Void>
    sendEmail(@NotNull EmailContent emailContent)
    {
        return
        Mono.defer(() -> {
            log.info(
                "Send email to: {}, subject: {}, body:{}",
                emailContent.getTo(), emailContent.getSubject(), emailContent.getTextBody()
            );

            return Mono.empty();
        });

        /*
         * 对于邮件发送过程中因为网络波动而出现的失败，
         * 有比固定时间重试（fixedDelay()）更好的策略，即指数退避。
         *
         * 比如代码中的调用：
         *
         * Retry.backoff(MAX_ATTEMPT_TIMES, Duration.ofSeconds(1))
         *      .maxBackoff(Duration.ofSeconds(10))
         *
         * 表明每失败一次，等待重试的时间就在原有的基础上乘以 2，
         * 具体如下表所示：
         *
         * ----------------------------------
         * 重试次数     等待重试时间（单位：秒）
         *    0             1
         *    1             2
         *    2             4
         *    3             8
         *    4             10
         *    5             10
         * -----------------------------------
         *
         * maxBackoff() 则给重试时间封了顶，
         * 不论重试多少次，等待时间都不会超过 10 秒。
         */
//        final Retry retryStrategy
//            = Retry.backoff(MAX_ATTEMPT_TIMES, Duration.ofSeconds(1))
//                   .maxBackoff(Duration.ofSeconds(10))
//                   .filter(this::isRetryableError)
//                   .doBeforeRetry(retrySignal -> {
//                        // 记录尝试次数和失败原因
//                        log.warn(
//                            "Retry attempt {} for email to {}.",
//                            retrySignal.totalRetries() + 1,
//                            emailContent.getTo()
//                        );
//                   });
//
//        return EmailFormatVerifier
//            .isValidEmail(emailContent.getTo()).then(
//                this.getEmailPublisherInfo()
//                    .switchIfEmpty(
//                        Mono.error(
//                            new EmailException(
//                                EmailException.ErrorType.CONFIG_MISSING,
//                                "Missing email publisher info!", null
//                            )
//                        )
//                    )
//                    .flatMap(tuple ->
//                        this.sendEmailReactive(
//                            emailContent,
//                            tuple.getT1(), tuple.getT2()
//                        )
//                    )
//                    .timeout(Duration.ofSeconds(30L))
//                    .retryWhen(retryStrategy)
//                    .onErrorResume(exception -> {
//                        String errorMessage
//                            = format(
//                            "Send email to %s finally failed! MAX_ATTEMPT_TIMES = %d. Cause: %s",
//                            emailContent.getTo(), MAX_ATTEMPT_TIMES,
//                            exception.getMessage()
//                        );
//
//                        log.error(errorMessage, exception);
//
//                        return Mono.error(
//                            new EmailException(
//                                EmailException.ErrorType.NETWORK_ISSUE,
//                                errorMessage, exception
//                            )
//                        );
//                    })
//            );
    }
}