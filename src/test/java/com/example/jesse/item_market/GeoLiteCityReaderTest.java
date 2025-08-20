package com.example.jesse.item_market;

import com.example.jesse.item_market.location_search.GeoLiteCityReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

/** 地理信息相关 CSV 文件读取并存储至 Redis 实现。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GeoLiteCityReaderTest
{
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GeoLiteCityReader geoLiteCityReader;

    @Test
    @Order(1)
    public void readBlocksFromFileTest()
    {
        this.geoLiteCityReader
            .readBlocksFromFile().block();
    }

    @Test
    @Order(2)
    public void readLocationFromFileTest()
    {
        this.geoLiteCityReader
            .readLocationFromFile().block();
    }

    /** 最后调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
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
