package com.example.jesse.item_market;

import com.example.jesse.item_market.email.dto.EmailContent;
import com.example.jesse.item_market.email.utils.VerifyCodeGenerator;
import com.example.jesse.item_market.email_send_task.dto.EmailTaskDTO;
import com.example.jesse.item_market.email_send_task.dto.TaskPriority;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

/**
 * EmailContent 类的序列化（Serialization）和
 * 反序列化（Deserialization）测试。
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmailTaskSerializationTest
{
    private final static String VARIFY_CONTENT_JSON_KEY
        = "varifyContentJson";

    private final static String ATTACHMENT_EMAIL_CONTENT_JOSN_KEY
        = "attachmentEmailContent";

    private final ObjectMapper mapper
        = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Test
    @Order(1)
    public void TestSerialization()
    {
        EmailContent varifyContent
            = EmailContent.fromVarify(
                "Peter-Griffin",
                "Peter-Griffin233@gmail.com",
                VerifyCodeGenerator.generateVerifyCode(8).block(),
                Duration.ofMinutes(5L))
            .block();

        EmailContent attachmentEmailContent
            = EmailContent.formWithAttachment(
                "Peter-Griffin",
                "Peter-Griffin233@gmail.com",
                "Big News For You!",
                "The pictures of Peter-Griffin is published!",
                "E:\\图片素材\\Family-Guy Avatar\\Perter 头像.png")
            .block();

        EmailTaskDTO varifyEmailTask
            = EmailTaskDTO.create(
                TaskPriority.HIGH,
                varifyContent)
            .block();

        EmailTaskDTO attachmentEmailTask
            = EmailTaskDTO.create(
                TaskPriority.LOW,
                attachmentEmailContent)
            .block();

        try
        {
            String varifyContentJson
                = this.mapper.writeValueAsString(varifyEmailTask);

            String attachmentEmailContentJson
                = this.mapper.writeValueAsString(attachmentEmailTask);

            System.out.println(varifyContentJson);
            System.out.println(attachmentEmailContentJson);

            this.redisTemplate
                .opsForValue()
                .multiSet(
                    Map.of(
                        VARIFY_CONTENT_JSON_KEY, varifyContentJson,
                        ATTACHMENT_EMAIL_CONTENT_JOSN_KEY, attachmentEmailContentJson
                    )
                ).block();
        }
        catch (JsonProcessingException exception)
        {
            log.error(
                "Serialization instance of EmailTaskDTO failed! Caused by: {}",
                exception.getMessage(), exception
            );
        }
    }

    @Test
    @Order(2)
    public void TestDeserialization()
    {
        List<String> contentJsons
            = this.redisTemplate
                  .opsForValue()
                  .multiGet(List.of(VARIFY_CONTENT_JSON_KEY, ATTACHMENT_EMAIL_CONTENT_JOSN_KEY))
                  .flatMapMany(Flux::fromIterable)
                  .map(String::valueOf)
                  .collectList()
                  .block();

        Assertions.assertNotNull(contentJsons);

        contentJsons.forEach((contentJson) -> {
            try
            {
                EmailTaskDTO content
                    = this.mapper.readValue(contentJson, EmailTaskDTO.class);

                System.out.println(content);
            }
            catch (JsonProcessingException exception)
            {
                log.error(
                    "Deserialization instance of EmailTaskDTO failed! Caused by: {}",
                    exception.getMessage(), exception
                );
            }
        });
    }

    @Test
    @Order(3)
    public void
    getTimestampFromRedis()
    {
        Flux.interval(Duration.ofSeconds(2L))
            .flatMap((ignore) ->
                this.redisTemplate
                    .getConnectionFactory()
                    .getReactiveConnection()
                    .serverCommands()
                    .time(MICROSECONDS)
                    .timeout(Duration.ofSeconds(3L))
                    .map((time) -> {
                        double stamp = (time.doubleValue() / (1000.00 * 1000.00));
                        return Math.round(stamp * 1_000_000) / 1_000_000.0;
                    })
                    .cache(Duration.ofSeconds(1L))
                    .doOnSuccess((timestamp) ->
                        System.out.printf("%f%n", timestamp))
                    .onErrorResume((exception) ->
                        redisGenericErrorHandel(exception, null)))
            .take(10)
            .blockLast();
    }

    @Test
    @Order(4)
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
