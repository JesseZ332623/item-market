package com.example.jesse.item_market;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

/** 调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
@Slf4j
@SpringBootTest
public class RedisClearTest
{
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Test
    public void redisFlushAllAsync()
    {
        redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll(RedisServerCommands.FlushOption.ASYNC)
            .doOnSuccess(System.out::println)
            .block();
    }
}
