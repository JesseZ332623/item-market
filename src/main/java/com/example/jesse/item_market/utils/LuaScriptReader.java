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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.example.jesse.item_market.utils.LuaScriptOperatorType.*;
import static java.lang.String.format;

/** Redis Lua 脚本读取器实现。*/
@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class LuaScriptReader
{
    /** 本项目 Lua 脚本根目录。*/
    private static final String
    LUA_SCRIPT_CLASSPATH_PREFIX = "lua-script/";

    /** 本项目 Lua 脚本路径所在。*/
    @Value("${app.lua-script-path}")
    private String luaScriptPath;

    /** 脚本的读取模式。*/
    @Value("${app.lua-script-mode}")
    private String scriptMode;

    /** 从文件系统中读取脚本时用于拼合脚本路径。*/
    public @NotNull Path
    getFullScriptPath(
        @NotNull LuaScriptOperatorType operatorType,
        @NotNull String luaScriptName
    )
    {
        /* 对于从 JAR 中的相对路径获取资源的情况下，这个方法不可用。*/
        if ("classpath".equals(scriptMode))
        {
            throw new UnsupportedOperationException(
                "Path operator not supported in <classpath> mode!"
            );
        }

        return Path.of(luaScriptPath)
                   .resolve(operatorType.getTypeName())
                   .resolve(luaScriptName)
                   .normalize();
    }

    /** 从文件系统中加载脚本。（开发、测试时用）*/
    @Contract("_, _ -> new")
    private @NotNull
    DefaultRedisScript<LuaOperatorResult>
    loadFromFileSystem(LuaScriptOperatorType operatorType, String luaScriptName) throws IOException
    {
        Path scriptPath
            = this.getFullScriptPath(operatorType, luaScriptName);

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
    }

    /** 从 JAR 中的相对路径加载脚本（生产环境用）。*/
    @Contract("_, _ -> new")
    private @NotNull
    DefaultRedisScript<LuaOperatorResult>
    loadFromClassPath(@NotNull LuaScriptOperatorType operatorType, String luaScriptName) throws IOException
    {
            String classpathPath
                = LUA_SCRIPT_CLASSPATH_PREFIX +
                operatorType.getTypeName()  +
                "/"                         +
                luaScriptName;

            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(classpathPath))
            {
                if (inputStream == null)
                {
                    throw new LuaScriptOperatorFailed(
                        format("Lua script: %s not found in classpath!",
                            classpathPath), null);
                }

                String scriptContent
                    = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                return new DefaultRedisScript<>(scriptContent, LuaOperatorResult.class);
            }
    }

    /**
     * 根据配置，从不同的源读取脚本。
     *
     * @param operatorType  Lua 脚本类型
     * @param luaScriptName Lua 脚本名
     */
    public @NotNull Mono<DefaultRedisScript<LuaOperatorResult>>
    fromFile(LuaScriptOperatorType operatorType, String luaScriptName)
    {
        return Mono.fromCallable(() -> {
            if ("classpath".equals(this.scriptMode))
            {
                return this.loadFromClassPath(operatorType, luaScriptName);
            }
            else
            {
                return this.loadFromFileSystem(operatorType, luaScriptName);
            }
        }).onErrorMap(
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
