package com.example.jesse.item_market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 应用启动类。*/
@EnableKafka
@EnableScheduling
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.example.jesse.item_market",
        "io.github.jessez332623"
    }
)
public class ItemMarketApplication
{
	public static void main(String[] args) {
		SpringApplication.run(ItemMarketApplication.class, args);
	}
}
