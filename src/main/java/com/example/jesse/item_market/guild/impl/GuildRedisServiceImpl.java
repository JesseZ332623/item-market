package com.example.jesse.item_market.guild.impl;

import com.example.jesse.item_market.guild.GuildRedisService;
import com.example.jesse.item_market.guild.utils.PrefixRange;
import com.example.jesse.item_market.lock.RedisLock;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private RedisLock redisLock;

    /** 按公会名搜索所有公会成员的 UUID。*/
    @Override
    public Flux<String>
    findAllMembersByGuildName(@NotNull String guildName)
    {
        final String guildNameSetKey = getGuildNameSetKey();
        final String formatGuildName = guildName.trim().replace(' ', '-');
        final String usersPattern    = "users:[0-9]*";

        Mono<Boolean> checkGuildNameExists
            = this.redisTemplate
                  .opsForSet()
                  .isMember(guildNameSetKey, formatGuildName);

        Flux<String> findAllGuildMembers
            = this.redisTemplate
                  .scan(
                      ScanOptions.scanOptions()
                          .match(usersPattern)
                          .build())
                  .flatMap((matchedUserKeys) ->
                      this.redisTemplate
                          .opsForHash()
                          .get(matchedUserKeys, "\"guild\"")
                          .map((res) -> (String) res)
                          .filter((queryGuildName) ->
                              queryGuildName.equals(formatGuildName))
                          .map((ignore) -> matchedUserKeys.split(":")[1])
                  );

        return checkGuildNameExists
            .flatMapMany(exists -> {
                if (!exists)
                {
                    return Flux.error(
                        new IllegalArgumentException(
                            format("Guild name: %s not exist!", formatGuildName)
                        )
                    );
                }

                return findAllGuildMembers;
            })
            .timeout(Duration.ofSeconds(3L))
            .onErrorResume(exception ->
                redisGenericErrorHandel(exception, null));
    }

    /** 按公会名搜索该工会 Leader 的 uuid。*/
    @Override
    public Mono<String>
    findLeaderIdByGuildName(@NotNull String guildName)
    {
        return
        this.findAllMembersByGuildName(guildName)
            .flatMap((memberId) -> 
                this.redisTemplate
                    .opsForHash()
                    .get(getUserKey(memberId), "\"guild-role\"")
                    .map(String::valueOf)
                    .filter("Leader"::equals)
                    .map((role) -> memberId))
            .next()
            .timeout(Duration.ofSeconds(3L))
            .doOnSuccess(System.out::println)
            .onErrorResume(exception ->
                redisGenericErrorHandel(exception, null));
    }

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

        final String guildKey        = getGuildKey(formatGuildName);
        final String guildLogKey     = getGuildLogKey();
        final String guildNameSetKey = getGuildNameSetKey();
        final String userKey         = getUserKey(uuid);
        final int    MAX_MEMBERS     = 500;

        return
        this.luaScriptReader
            .fromFile(GUILD_OPERATOR, "joinGuild.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(guildKey, guildLogKey, guildNameSetKey, userKey),
                        uuid, formatGuildName,
                        USER_GUILD_FIELD, USER_GUILD_ROLE_FIELD, MAX_MEMBERS)
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

                            case "GUILD_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Guild: %s not exist!", formatGuildName)
                                    )
                                );

                            case "USER_NAME_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Query user name by: %s not found!", uuid)
                                    )
                                );

                            case "GUILD_IS_FULL" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format(
                                            "Guild: %s is full! (Max mamber = %d)",
                                            guildName, MAX_MEMBERS
                                        )
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
    public Mono<List<String>>
    fetchAutoCompleteMember(@NotNull String guildName, String prefix)
    {
        return
        PrefixRange.create(prefix)
            .flatMap((prefixRange) -> {
                System.out.printf(
                    "Names with <%s> prefix from guild [%s]%n",
                    prefix, guildName
                );

                System.out.printf(
                    "Range of min: %s, Range of max: %s%n",
                    prefixRange.getMin(), prefixRange.getMax()
                );

                final String guildKey
                    = getGuildKey(guildName.trim().replace(' ', '-'));

                return this.redisTemplate
                           .opsForZSet()
                           .rangeByLex(
                               guildKey,
                               Range.rightOpen(prefixRange.getMin(), prefixRange.getMax()))
                           .map(String::valueOf)
                           .collectList()
                           .timeout(Duration.ofSeconds(3L));
            })
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, Collections.emptyList())
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
    sendMessageBetweenMembers(
        String guildName,
        String sender, String receiver,
        String message)
    {
        return null;
    }

    /**
     * Leader 解散公会，[并向所有公会成员发送解散的消息](这一步后续再研究)。
     *
     * @param uuid  公会创始人 ID
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    deleteGuild(String uuid)
    {
        return
        this.redisLock.withLock(
            "DeleteGuild_Lock",
            10L, 10L,
            (identifier) -> {
                log.info("Trying to delete guild for user: {}", uuid);
                log.info("Destribute lock identifier of DeleteGuild_Lock: {}", identifier);

                final String guildLogKey        = getGuildLogKey();
                final String guildNameSetKey    = getGuildNameSetKey();
                final String guildNameSetLogKey = getGuildNameSetLogKey();
                final String leaderUserKey      = getUserKey(uuid);

                Mono<Boolean> isLeader
                    = this.redisTemplate
                          .opsForHash()
                          .get(leaderUserKey, "\"guild-role\"")
                          .map(String::valueOf)
                          .map("Leader"::equals);

                Mono<String> getGuildName
                    = this.redisTemplate
                          .opsForHash()
                          .get(leaderUserKey, "\"guild\"")
                          .map(String::valueOf)
                          .cache(Duration.ofSeconds(15L));

                Mono<Void> deleteGuildNameFromSet
                 = getGuildName
                    .flatMap((guildName) ->
                        this.redisTemplate
                            .opsForSet()
                            .remove(guildNameSetKey, guildName))
                    .then();

                Mono<Void> deleteGuild
                    = getGuildName
                        .flatMap((guildName) ->
                            this.redisTemplate
                                .opsForZSet()
                                .delete(getGuildKey(guildName)))
                        .then();

                Mono<Void> resetGuildInfoForMembers
                    = getGuildName
                        .flatMapMany(this::findAllMembersByGuildName)
                        .flatMap((memberId) ->
                            this.redisTemplate.opsForHash()
                                .putAll(
                                    getUserKey(memberId),
                                    Map.of(
                                        "\"guild\"", "---",
                                        "\"guild-role\"", "---"
                                    )
                                ))
                    .then();

                Mono<Void> recordGuildNameDeleteLog
                    = getGuildName.flatMap((guildName) -> {
                        final Map<String, String> deleteGuildNameLogInfo
                            = Map.of(
                            "event", "REMOVE_GUILD_NAME",
                            "guild-name", guildName,
                            "timestamp", String.valueOf(Instant.now().getEpochSecond())
                        );

                    MapRecord<String, String, String> record
                           = StreamRecords.newRecord()
                             .ofStrings(deleteGuildNameLogInfo)
                             .withStreamKey(guildNameSetLogKey);

                    return this.redisTemplate
                               .opsForStream()
                               .add(record).then();
                });

                Mono<Void> recordGuildDeleteLog
                    = getGuildName.flatMap((guildName) -> {
                        final Map<String, String> deleteGuildLogInfo
                            = Map.of(
                            "event", "DELETE_GUILD",
                            "uuid", uuid,
                            "guild-name", guildName,
                            "timestamp", String.valueOf(Instant.now().getEpochSecond())
                        );

                        MapRecord<String, String, String> record
                            = StreamRecords.newRecord()
                                .ofStrings(deleteGuildLogInfo)
                                .withStreamKey(guildLogKey);

                        return this.redisTemplate
                                   .opsForStream()
                                   .add(record).then();
                    });

                return
                isLeader.flatMap((is) -> {
                    if (!is) {
                        return Mono.error(
                            new IllegalArgumentException(
                                format("User: %s not a leader of guild!", uuid)
                            )
                        );
                    }

                    return
                    resetGuildInfoForMembers
                        .then(
                            Mono.when(
                                deleteGuildNameFromSet,
                                deleteGuild,
                                recordGuildNameDeleteLog,
                                recordGuildDeleteLog
                            )
                        );
                })
                .timeout(Duration.ofSeconds(30L))
                .onErrorResume((exception) ->
                    redisGenericErrorHandel(exception, null));
            }
        );
    }
}
