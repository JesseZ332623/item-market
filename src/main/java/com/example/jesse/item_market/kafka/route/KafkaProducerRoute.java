package com.example.jesse.item_market.kafka.route;

import com.example.jesse.item_market.kafka.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/** Kafka 测试服务路由函数配置类。*/
@Slf4j
@Configuration
public class KafkaProducerRoute
{
    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Bean
    public RouterFunction<ServerResponse>
    kafkaProducerRouterFunction()
    {
        return RouterFunctions
            .route()
            .POST("/api/kafka/send-test-message", this.kafkaProducerService::sendMessage)
            .POST("/api/kafka/send-location",     this.kafkaProducerService::sendLocationMessage)
            .build();
    }
}
