package com.example.jesse.item_market.utils;

import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import com.example.jesse.item_market.utils.exception.LuaScriptOperatorFailed;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.String.format;

/** Redis Lua 脚本读取器实现。*/
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class LuaScriptReader
{
    /** 本项目 Lua 脚本路径所在。*/
    @Value("${app.lua-script-path}")
    private String luaScriptPath;

    @Contract("_ -> new")
    public @NotNull Mono<DefaultRedisScript<LuaOperatorResult>>
    fromFile(String luaScriptName)
    {
        return Mono.fromCallable(() -> {
            Path scriptPath = Path.of(luaScriptPath)
                .resolve(luaScriptName).normalize();

            if (!Files.exists(scriptPath))
            {
                throw new LuaScriptOperatorFailed(
                    format("Lua script: %s not found!", luaScriptName), null
                );
            }

            return new DefaultRedisScript<>(
                Files.readString(scriptPath, StandardCharsets.UTF_8),
                LuaOperatorResult.class
            );
        })
        .onErrorMap(
            IOException.class,
            (exception) ->
                new LuaScriptOperatorFailed(
                    format(
                        "Load lua script %s failed! Caused by: %s",
                        luaScriptName, exception.getMessage()
                    ), null
                )
        );
    }
}
