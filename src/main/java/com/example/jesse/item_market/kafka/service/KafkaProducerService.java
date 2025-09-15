package com.example.jesse.item_market.kafka.service;

import com.example.jesse.item_market.kafka.dto.LocationInfo;
import com.example.jesse.item_market.response.ResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

import static com.example.jesse.item_market.response.URLParamPrase.praseRequestParam;

/** Kafka 生产者测试。*/
@Slf4j
@Service
public class KafkaProducerService
{
    /** Kafka 模板类。*/
    @Autowired
    private
    KafkaTemplate<String, Object> kafkaTemplate;

    /** 通用响应构建器。*/
    @Autowired
    private ResponseBuilder responseBuilder;

    /**
     * 生产者向指定主题发送指定消息。
     * 发送消息的操作是异步的，它会立刻返回一个 {@link CompletableFuture}
     */
    public Mono<ServerResponse>
    sendMessage(@NotNull ServerRequest request)
    {
        return
        Mono.zip(
            praseRequestParam(request, "topic"),
            praseRequestParam(request, "message"))
        .flatMap((params) -> {
            final String topicName = params.getT1();
            final String message   = params.getT2();

            /*
            * kafkaTemplate 的操作返回完全期值，
            * 需要拿 Mono.fromFuture() 包装一下，以便处理消息发送的成功成功和失败。
            */
            return
            Mono.fromFuture(() ->
                this.kafkaTemplate
                    .send(topicName, message)
                    .thenApply((sendResult) -> {
                        String successResMsg
                            = String.format(
                            "[SEND MESSAGE] Send message: %s to topic: %s.",
                            message, topicName
                        );

                        log.info(successResMsg);

                        return successResMsg;
                    })
            ).flatMap((successMsg) ->
                this.responseBuilder
                    .OK(null, successMsg, null, null)
            );
        })
        .onErrorResume(IllegalArgumentException.class,
            (exception) ->
            this.responseBuilder
                .BAD_REQUEST(exception.getMessage(), exception))
        .onErrorResume((exception) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
        );
    }

    public Mono<ServerResponse>
    sendLocationMessage(@NotNull ServerRequest serverRequest)
    {
        return
        Mono.zip(
            praseRequestParam(serverRequest, "topic"),
            serverRequest.bodyToMono(LocationInfo.class))
        .flatMap((params) ->
        {
            final String topicName          = params.getT1();
            final LocationInfo locationInfo = params.getT2();
            final String messageKey         = locationInfo.getLocationId();

            return
            Mono.fromFuture(
                () -> this.kafkaTemplate
                          .send(topicName, messageKey, locationInfo)
                          .thenApply((sendRes) -> {
                              String successResMsg
                                  = String.format(
                                  "[SEND MESSAGE] Send location info to topic: %s.",
                                  topicName
                              );

                              log.info(successResMsg);

                              return successResMsg;
                          })
            ).flatMap((successMsg) ->
                this.responseBuilder
                    .OK(
                        null, successMsg,
                        null, null
                    )
            );
        })
        .onErrorResume(IllegalArgumentException.class,
            (exception) ->
                this.responseBuilder
                    .BAD_REQUEST(exception.getMessage(), exception))
        .onErrorResume((exception) ->
            this.responseBuilder
                .INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
        );
    }
}