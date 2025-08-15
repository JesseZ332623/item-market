package com.example.jesse.item_market.email;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email.exception.EmailException;
import com.example.jesse.item_market.email.service.EmailSenderInterface;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static com.example.jesse.item_market.email.exception.EmailException.ErrorType.INVALID_CONTENT;

/** 邮件发送器测试用实现。*/
@Slf4j
@Service(value = "TestImpl")
public class EmailSenderTestImpl implements EmailSenderInterface
{
    /**
     * 外部可调用的发送邮件的方法（本实现用于测试模拟）。
     *
     * @param emailContent 邮件内容
     *
     * @return 表示操作是否正确完成的响应式流
     */
    @Override
    public Mono<Void>
    sendEmail(@NotNull EmailContent emailContent)
    {
        return
        Mono.defer(() -> {
            if (ThreadLocalRandom.current().nextInt(100) > 10)
            {
                log.info("Send email success, content: {}", emailContent);
                return Mono.empty();
            }
            else
            {
                String errMessage
                    = format("Send email to: %s failed!", emailContent.getTo());

                log.error(errMessage);

                return
                Mono.error(
                    new EmailException(INVALID_CONTENT, errMessage, null)
                );
            }
        });
    }
}
