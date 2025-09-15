package com.example.jesse.item_market.errorhandle;

import com.example.jesse.item_market.utils.exception.LuaScriptOperatorFailed;
import io.lettuce.core.RedisCommandTimeoutException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.SerializationException;
import reactor.core.publisher.Mono;

/** 本项目所有的 Redis 操作中，通用的错误处理方法工具类。*/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class RedisErrorHandle
{
    /**
     * 本项目所有的 Redis 操作中，通用的错误处理方法。
     *
     * @param <T> 要发布数据或异常类型
     *
     * @param exception     Redis 操作中可能抛出的异常
     * @param fallbackValue 出错后可能需要返回的默认值
     *
     * @return 发布异常或者默认值的 Mono
     */
    public static <T> @NotNull Mono<T>
    redisGenericErrorHandel(@NotNull Throwable exception, T fallbackValue)
    {
        switch (exception)
        {
            case LuaScriptOperatorFailed luaScriptOperatorFailed ->
                log.error(luaScriptOperatorFailed.getMessage(), exception);

            case RedisConnectionFailureException redisConnectionFailureException ->
                log.error(
                    "Redis connect failed!",
                    redisConnectionFailureException);

            case RedisCommandTimeoutException redisCommandTimeoutException ->
                log.warn(
                    "Redis operator time out!",
                    redisCommandTimeoutException);

            case SerializationException serializationException ->
                log.error(
                    "Data deserialization failed!",
                    serializationException);

            case DataAccessException dataAccessException ->
                log.error(
                    "Spring data access failed!", dataAccessException);

            /*
             * 对于上述系统级别异常之外的业务异常，
             * 需要保留原始异常，不得 re-throw 成 ProjectRedisOperatorException
             */
            default -> {
                return
                (fallbackValue != null)
                    ? Mono.just(fallbackValue)
                    : Mono.error(exception);
            }
        }

        /* 若 fallbackValue 的值为空，异常会 re-throw 然后向上传递。*/
        return (fallbackValue != null)
            ? Mono.just(fallbackValue)
            : Mono.error(
                new ProjectRedisOperatorException(
                    exception.getMessage(), exception
                )
            );
    }
}
