package com.example.jesse.item_market.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Jackson 配置类。*/
@Configuration
public class JacksonConfig
{
    @Bean
    @Primary
    public ObjectMapper httpObjectMapper() {
        return new ObjectMapper();
    }

    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();

        mapper.activateDefaultTyping(
            mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }
}
