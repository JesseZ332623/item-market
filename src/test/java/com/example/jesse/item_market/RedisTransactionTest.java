package com.example.jesse.item_market;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

/** Redis 连接测试类。*/
@Slf4j
@SpringBootTest
public class RedisTransactionTest
{
    @Autowired
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Test
    public void TestRedisConnection()
    {
        // 检查是否可用
        reactiveRedisTemplate
            .execute(ReactiveRedisConnection::ping)
            .subscribe(System.out::println); // 应输出 "PONG"
    }
}