package com.example.jesse.item_market.guild.impl;

import com.example.jesse.item_market.guild.GuildRedisService;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

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
    joinGuild(String uuid, String guildName) {
        return null;
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
    leaveGuild(String uuid, String guildName) {
        return null;
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
    fetchAutoCompleteMember(String guildName, String prefix) {
        return null;
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
    public Mono<Void> sendMessageBetweenMembers(String guildName, String sender, String receiver, String message) {
        return null;
    }

    /**
     * 用户删除公会，并向所有公会成员发送解散的消息。
     *
     * @param uuid      公会创始人 ID
     * @param guildName 公会名
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void> deleteGuild(String uuid, String guildName) {
        return null;
    }
}
