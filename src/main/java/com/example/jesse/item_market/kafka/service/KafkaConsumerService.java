package com.example.jesse.item_market.kafka.service;

import com.example.jesse.item_market.kafka.dto.LocationInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/** Kafka 消费者测试。*/
@Slf4j
@Service
public class KafkaConsumerService
{
    @KafkaListener(topics = {"test-topic"}, groupId = "user-group")
    public void
    testMessageListen(
        String message,
        @NotNull
        ConsumerRecord<String, String> consumerRecord)
    {
        log.info(
            "[RECEIVE MESSAGE] Received message: {} " +
            "in group: test-topic, partition: {}, timestamp: {}",
            message, consumerRecord.partition(), consumerRecord.timestamp()
        );
    }

    @KafkaListener(topics = {"location-topic"}, groupId = "user-group")
    public void
    locationDataListen(
        LocationInfo locationInfo,
        @NotNull
        ConsumerRecord<String, LocationInfo> consumerRecord)
    {
        log.info(
            "[RECEIVE DATA] " +
            "Received location info in group: location-topic, partition: {}, timestamp: {}",
            consumerRecord.partition(), consumerRecord.timestamp()
        );

        System.out.println("Location: " + locationInfo);
    }
}