package com.example.jesse.item_market;

import com.example.jesse.item_market.guild.GuildRedisService;
import com.example.jesse.item_market.user.UserRedisService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** 公会操作功能测试。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GuildOperatorTest
{
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private
    UserRedisService userRedisService;

    @Autowired
    private
    GuildRedisService guildRedisService;

    final private static
    List<String> NAME_PREFIX_BY_Je
        = List.of(
            "Jeremy", "Jesse", "Jeffrey", "Jenson", "Jerome",
            "Jethro", "Jerald", "Jevon", "Jediah", "Jensen"
        );

    final private static
    List<String> NAME_PREFIX_BY_Mi
        = List.of(
            "Michael", "Miles", "Mitchell", "Milo", "Milton",
            "Mickey", "Misha", "Miran", "Mica", "Mirek"
        );

    final private static
    List<String> NAME_PREFIX_BY_Alt
        = List.of(
            "Altair", "Alton", "Alter", "Altin", "Altus",
            "Altara", "Altina", "Altrix", "Altem", "Altaro"
        );

    final private static
    List<String> TEST_PREFIX = List.of("Je", "Mi", "Alt");

    final private static
    List<String> TEST_GUILD_NAME
        = List.of(
            "Council of the Black Harvest",
            "The Dark Brotherhood", "Gank and Spank"
        );

    @SafeVarargs
    public final List<String>
    mergeAndShuffleList(List<String> ...lists)
    {
        List<String> mergedList
            = Stream.of(lists)
                    .flatMap(List::stream)
                    .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(mergedList);
        Collections.shuffle(mergedList);

        return mergedList;
    }

    private List<String>
    createUsersFromList(@NotNull List<String> users)
    {
        return
        Flux.fromIterable(users)
            .flatMap((name) ->
                this.userRedisService.addNewUser(name))
            .collectList()
            .block();
    }

    private void
    createGuildFromListHead(
        @NotNull List<String> uuids,
        @NotNull List<String> guildNames
    )
    {
        int maxLength = Math.max(uuids.size(), guildNames.size());

        Set<Map.Entry<String, String>> testData
            = IntStream.range(0, maxLength)
                       .boxed()
                       .collect(Collectors.toMap(uuids::get, guildNames::get))
                       .entrySet();

        Flux.fromStream(testData.stream())
            .flatMap((entry) ->
                this.guildRedisService
                    .createGuild(entry.getKey(), entry.getValue()))
            .blockLast();
    }

    private void
    joinGuildFromListExpectHead(
        @NotNull List<String> uuids,
        @NotNull String       guildName
    )
    {
        uuids.forEach((uuid) ->
            this.guildRedisService
                .joinGuild(uuid, guildName)
                .block()
        );
    }

    private void
    testFetchAutoCompleteMemberHandle(
        @NotNull List<String> guildNames,
        @NotNull List<String> prefixes
    )
    {
        int maxLength = Math.max(guildNames.size(), prefixes.size());

        Set<Map.Entry<String, String>> testData
            = IntStream.range(0, maxLength)
            .boxed()
            .collect(Collectors.toMap(guildNames::get, prefixes::get))
            .entrySet();

        Flux.fromStream(testData.stream())
            .flatMap((entry) ->
                this.guildRedisService
                    .fetchAutoCompleteMember(entry.getKey(), entry.getValue())
                    .doOnSuccess(System.out::println))
            .blockLast();
    }

    @Order(1)
    @Test
    public void TestFetchAutoCompleteMember()
    {
        // 1. 创建所有用户
        List<String> uuidsOfJePrefix
            = createUsersFromList(NAME_PREFIX_BY_Je);

        List<String> uuidsOfMiPrefix
            = createUsersFromList(NAME_PREFIX_BY_Mi);

        List<String> uuidOfAltPrefix
            = createUsersFromList(NAME_PREFIX_BY_Alt);

        // 2. 令各个用户列表的第一个用户创建公会，并成为该公会的 Leader
        createGuildFromListHead(
            List.of(
                uuidsOfJePrefix.getFirst(),
                uuidsOfMiPrefix.getFirst(),
                uuidOfAltPrefix.getFirst()
            ), TEST_GUILD_NAME
        );

        /* 合并各个用户列表除第一个用户之外的其他所有用户为一个新不变列表，然后打乱。*/
        List<String> mergedNames
            = this.mergeAndShuffleList(
                uuidsOfJePrefix.stream().skip(1L).toList(),
                uuidsOfMiPrefix.stream().skip(1L).toList(),
                uuidOfAltPrefix.stream().skip(1L).toList()
            );

        int namesAvgLen = mergedNames.size() / 3;

        List<String> partA = mergedNames.subList(0, namesAvgLen);
        List<String> partB = mergedNames.subList(namesAvgLen, namesAvgLen * 2);
        List<String> partC = mergedNames.subList(namesAvgLen * 2, mergedNames.size());

        // 3. 令各个用户列表除第一个用户之外的其他所有用户，加入对应的公会
        joinGuildFromListExpectHead(
            partA, TEST_GUILD_NAME.getFirst()
        );

        joinGuildFromListExpectHead(
            partB, TEST_GUILD_NAME.get(1)
        );

        joinGuildFromListExpectHead(
            partC, TEST_GUILD_NAME.get(2)
        );

        // 4. 最关键的测试环节
        // testFetchAutoCompleteMemberHandle(TEST_GUILD_NAME, TEST_PREFIX);
    }

    @Order(2)
    @Test
    void TestFindAllMembersByGuildName()
    {
        Flux.fromIterable(TEST_GUILD_NAME)
            .flatMap((guildName) -> {
                log.info("{}", guildName);

                return
                this.guildRedisService
                    .findAllMembersByGuildName(guildName)
                    .collectList()
                    .doOnSuccess(System.out::println);
            }).blockLast();
    }

    /** 删除公会的操作尝试使用了 Redis 分布式锁，所以需要单独提出来测试。 */
    @Order(3)
    @Test
    void TestDeleteGuild()
    {
        Flux.fromIterable(TEST_GUILD_NAME)
            .concatMap((guildName) ->
                this.guildRedisService
                    .findLeaderIdByGuildName(guildName)
                    .flatMap((leaderId) ->
                        this.guildRedisService.deleteGuild(leaderId))
            ).blockLast();
    }

    /** 最后调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
    @Order(4)
    @Test
    public void redisFlushAllAsync()
    {
        this.redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll(RedisServerCommands.FlushOption.ASYNC)
            .doOnSuccess(System.out::println)
            .block();
    }
}