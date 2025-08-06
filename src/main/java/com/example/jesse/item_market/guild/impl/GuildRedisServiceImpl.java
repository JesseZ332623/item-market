package com.example.jesse.item_market.guild.impl;

import com.example.jesse.item_market.guild.GuildRedisService;
import com.example.jesse.item_market.guild.dto.FetchAutoCompleteMemberResult;
import com.example.jesse.item_market.guild.utils.PrefixRange;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import com.example.jesse.item_market.utils.exception.LuaScriptOperatorFailed;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.utils.LuaScriptOperatorType.GUILD_OPERATOR;
import static com.example.jesse.item_market.utils.KeyConcat.*;
import static java.lang.String.format;

/** 公会 Redis 操作实现类。*/
@Slf4j
@Service
public class GuildRedisServiceImpl implements GuildRedisService
{
    /** Lua 脚本读取工具。*/
    @Autowired
    private LuaScriptReader luaScriptReader;

    /** 通用 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 专门为 LUA 脚本配置的 Redis 模板。*/
    @Autowired
    private
    ReactiveRedisTemplate<String, LuaOperatorResult> redisScriptTemplate;

    /**
     * 用户创建公会，并成为这个公会的 Leader。
     *
     * @param uuid      哪个用户要创建公会？
     * @param guildName 新公会的名字是？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    createGuild(String uuid, @NotNull String guildName)
    {
        /*
         * 虽然 Redis key 技术上支持空格，
         * 但是从维护性考虑，还是全部替换成短横杠更合适。
         */
        String formatGuildName
            = guildName.trim().replace(' ', '-');

        final String guildKey           = getGuildKey(formatGuildName);
        final String guildNameSetKey    = getGuildNameSetKey();
        final String guildLogKey        = getGuildLogKey();
        final String guildNameSetLogKey = getGuildNameSetLogKey();
        final String userKey            = getUserKey(uuid);

        return
        this.luaScriptReader
            .fromFile(GUILD_OPERATOR, "createGuild.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(
                            guildKey, guildNameSetKey, guildLogKey,
                            guildNameSetLogKey, userKey
                        ),
                        uuid, formatGuildName,
                        USER_GUILD_FIELD, USER_GUILD_ROLE_FIELD)
                    .timeout(Duration.ofSeconds(5L))
                    .next()
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "ALREADY_JOINED" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("User: %s already join guild!", uuid)
                                    )
                                );

                            case "DUPLICATE_GUILD_NAME" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Guild name: %s already exist!", formatGuildName)
                                    )
                                );

                            case "USER_NAME_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Query user name by: %s not found!", uuid)
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        })
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 用户加入公会。
     *
     * @param uuid      哪个用户要加入？
     * @param guildName 加入哪个公会？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    joinGuild(String uuid, @NotNull String guildName)
    {
        String formatGuildName
            = guildName.trim().replace(' ', '-');

        final String guildKey    = getGuildKey(formatGuildName);
        final String guildLogKey = getGuildLogKey();
        final String userKey     = getUserKey(uuid);

        return
        this.luaScriptReader
            .fromFile(GUILD_OPERATOR, "joinGuild.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(guildKey, guildLogKey, userKey),
                        uuid, formatGuildName,
                        USER_GUILD_FIELD, USER_GUILD_ROLE_FIELD)
                    .timeout(Duration.ofSeconds(5L))
                    .next()
                    .flatMap((result) ->
                        switch(result.getResult())
                        {
                            case "ALREADY_JOINED" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("User: %s already join guild!", uuid)
                                    )
                                );

                            case "USER_NAME_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Query user name by: %s not found!", uuid)
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        }))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 用户离开公会。
     *
     * @param uuid      哪个用户要离开？
     * @param guildName 离开哪个公会？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    leaveGuild(String uuid, @NotNull String guildName)
    {
        String formatGuildName
            = guildName.trim().replace(' ', '-');

        final String guildKey    = getGuildKey(formatGuildName);
        final String guildLogKey = getGuildLogKey();
        final String userKey     = getUserKey(uuid);

        return
        this.luaScriptReader
            .fromFile(GUILD_OPERATOR, "leaveGuild.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(guildKey, guildLogKey, userKey),
                        uuid, formatGuildName,
                        USER_GUILD_FIELD, USER_GUILD_ROLE_FIELD)
                    .timeout(Duration.ofSeconds(5L))
                    .next()
                    .flatMap((result) ->
                        switch(result.getResult())
                        {
                            case "NOT_JOIN_ANY_GUILD" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("User: %s not join any guild!", uuid)
                                    )
                                );

                            case "NOT_BELONG_TO_GUILD" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format(
                                            "User: %s not belong to guild %s!",
                                            uuid, formatGuildName
                                        )
                                    )
                                );

                            case "LEAVE_FORBIDDEN" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format(
                                            "User: %s is leader of guild %s, leave is forbidden!",
                                            uuid, formatGuildName
                                        )
                                    )
                                );

                            case "USER_NAME_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Query user name by: %s not found!", uuid)
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        })
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 在搜索公会成员时，
     * 对于搜索框输入的 prefix，自动地补全全部匹配的成员名。
     *
     * @param guildName 搜索哪个公会的成员？
     * @param prefix    搜索框输入的内容
     *
     * @return 发布布公会成员中所有以 prefix 开头的成员名的 Flux
     */
    @Override
    public Flux<String>
    fetchAutoCompleteMember(@NotNull String guildName, String prefix)
    {
        return
        PrefixRange.create(prefix)
            .flatMapMany((prefixRange) -> {
                final String guildKey
                    = getGuildKey(guildName.trim().replace(' ', '-'));

                final String predecessor
                    = prefixRange.getPredecessor();

                final String successor
                    = prefixRange.getSuccessor();

                /*
                 * 这个脚本并不返回 LuaOperatorResult 类型，
                 * 而是一个 List<Sting>，所以这里的脚本要手动构建。
                 *
                 * 从此处暴露一个重大问题：
                 * 以前为了方便测试，Lua 脚本使用的是本机的绝对路径，
                 * 但是在生产环境所有的脚本会被打包到 JAR 的指定路径中取，
                 * 因此使用绝对路径是行不通的，
                 * 等到后续需要开发前端页面时，Lua 脚本的读取模块就需要重构了。
                 */
                Path scriptPath
                    = luaScriptReader
                        .getFullScriptPath(
                            GUILD_OPERATOR,
                            "fetchAutoCompleteMember.lua"
                        );

                RedisScript<FetchAutoCompleteMemberResult> script;

                try
                {
                    script = RedisScript.of(
                        Files.readString(scriptPath, StandardCharsets.UTF_8),
                        FetchAutoCompleteMemberResult.class
                    );
                }
                catch (IOException e)
                {
                    throw new LuaScriptOperatorFailed(
                        "Lua script: fetchAutoCompleteMember.lua not found!",
                        null
                    );
                }

                return
                this.redisTemplate
                    .execute(script, List.of(guildKey), predecessor, successor)
                    .timeout(Duration.ofSeconds(5L))
                    .next()
                    .flatMapMany((members) ->
                        Flux.fromIterable(
                            members.getMatchedMembers()
                                   .stream()
                                   .filter((member) -> !member.contains("{"))
                                   .toList()
                        )
                    );
            })
            .onErrorResume(
                (excption) ->
                    redisGenericErrorHandel(excption, null)
            );
    }

    /**
     * 公会成员之间互发消息（可能用到 Redis 的 PUB 和 SUB），后续再研究。
     *
     * @param guildName 哪个公会？
     * @param sender    发送人
     * @param receiver  接收人
     * @param message   消息
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    sendMessageBetweenMembers(String guildName, String sender, String receiver, String message) {
        return null;
    }

    /**
     * Leader 删除公会，[并向所有公会成员发送解散的消息](这一步后续再研究)。
     *
     * @param uuid      公会创始人 ID
     * @param guildName 公会名
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    deleteGuild(String uuid, String guildName) {
        return null;
    }
}
