package com.example.jesse.item_market.config;

import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

/** 项目 Redis 配置类。*/
@Configuration
public class ReactiveRedisConfig
{
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /** Redis 响应式连接工厂配置类。 */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory
    reactiveRedisConnectionFactory()
    {
        // 1. 创建独立 Redis 配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);       // Redis 地址
        config.setPort(redisPort);           // Redis 端口

        // 密码
        config.setPassword(RedisPassword.of(redisPassword));

        // 2. 创建客户端配置
        LettuceClientConfiguration clientConfig
            = LettuceClientConfiguration.builder()
            .clientOptions(
                ClientOptions.builder()
                    .autoReconnect(true)
                    .socketOptions(
                        SocketOptions.builder()
                            .connectTimeout(Duration.ofSeconds(5L)) // 连接超时
                            .keepAlive(true) // 自动管理 TCP 连接存活
                            .build()
                    )
                    .timeoutOptions(
                        TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(10L)) // 操作超时
                            .build()
                    ).build()
            )
            .commandTimeout(Duration.ofSeconds(10L))  // 命令超时时间
            .shutdownTimeout(Duration.ofSeconds(3L))  // 关闭超时时间
            .build();

        // 3. 创建连接工厂
        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * Redis 响应式模板的构建。
     *
     * @param factory Redis 连接工厂，
     *                Spring 会自动读取配置文件中的属性去构建。
     *
     * @return 配置好的 Redis 响应式模板
     */
    @Bean
    public ReactiveRedisTemplate<String, Object>
    reactiveRedisTemplate(ReactiveRedisConnectionFactory factory)
    {
        /* Redis 键使用字符串进行序列化。 */
        RedisSerializer<String> keySerializer
            = new StringRedisSerializer();

        /* Redis 值使用 Jackson 进行序列化。 */
        Jackson2JsonRedisSerializer<Object> valueSerializer
            = new Jackson2JsonRedisSerializer<>(Object.class);

        /* Redis Hash Key / Value 的序列化。 */
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object>
            builder = RedisSerializationContext.newSerializationContext(keySerializer);

        /* 创建 Redis 序列化上下文，设置序列化方式。 */
        RedisSerializationContext<String, Object> context
            = builder.value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();

        /* 根据上述配置构建 ReactiveRedisTemplate。 */
        return new ReactiveRedisTemplate<>(factory, context);
    }

    /** 专门用于执行 Lua 脚本的 ReactiveRedisTemplate。*/
    @Bean
    public ReactiveRedisTemplate<String, LuaOperatorResult>
    marketTransactionRedisTemplate(ReactiveRedisConnectionFactory factory)
    {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();

        Jackson2JsonRedisSerializer<LuaOperatorResult> valueSerializer
            = new Jackson2JsonRedisSerializer<>(LuaOperatorResult.class);

        RedisSerializationContext<String, LuaOperatorResult> context
            = RedisSerializationContext.<String, LuaOperatorResult>
                newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}