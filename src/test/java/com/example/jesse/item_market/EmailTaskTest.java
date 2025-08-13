package com.example.jesse.item_market;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email_send_task.dto.TaskPriority;
import com.example.jesse.item_market.email_send_task.impl.EmailSendTaskImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.example.jesse.item_market.email.utils.VerifyCodeGenerator.generateVerifyCode;

/** 邮件任务执行器测试类。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmailTaskTest
{
    /** 通用响应式 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailSendTaskImpl emailSendTask;

    @Test
    @Order(1)
    public void TestAddEmailTask()
    {
        Mono<EmailContent> heighLevelMail
            = generateVerifyCode(8)
                .flatMap((varifyCode) ->
                    EmailContent.fromVarify(
                        "Peter-Griffin",
                        "Peter-Griffin233@gmail.com",
                        varifyCode,
                        Duration.ofMinutes(5L)
                    )
                );

        Mono<EmailContent> lowLevelMail
            = EmailContent.formWithAttachment(
                "Peter-Griffin",
                "Peter-Griffin233@gmail.com",
                "Big News For You!",
                "The pictures of Peter-Griffin is published!",
                "E:\\图片素材\\Family-Guy Avatar\\Perter 头像.png"
            );

        heighLevelMail.flatMap((content) ->
            this.emailSendTask
                .addEmailTask(content, TaskPriority.HIGH, Duration.ZERO))
            .block();

        lowLevelMail.flatMap((content) ->
            this.emailSendTask
                .addEmailTask(content, TaskPriority.LOW, Duration.ofSeconds(10L)))
            .block();
    }

    @Test
    @Order(3)
    public void redisFlushAllAsync()
    {
        this.redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll(RedisServerCommands.FlushOption.ASYNC)
            .doOnSuccess(System.out::println)
            .block();
    }
}

