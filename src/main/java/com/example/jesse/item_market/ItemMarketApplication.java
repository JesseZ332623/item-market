package com.example.jesse.item_market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 应用启动类。*/
@EnableScheduling
@SpringBootApplication
public class ItemMarketApplication
{
	public static void main(String[] args) {
		SpringApplication.run(ItemMarketApplication.class, args);
	}
}
