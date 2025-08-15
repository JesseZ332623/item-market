package com.example.jesse.item_market;

import com.example.jesse.item_market.dto.UserLogDTO;
import com.example.jesse.item_market.guild.GuildRedisService;
import com.example.jesse.item_market.user.UserRedisService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
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
    ReactiveRedisTemplate<String, String> stringRedisTemplate;

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
    List<String> NAME_PREFIX_BY_Fe
        = List.of("Felix", "Fenris", "Fenton", "Fergus", "Feron");

    final private static
    List<String> NAME_PREFIX_BY_Lsa
        = List.of("Lsander", "Lsara", "Lsabella", "Lsandro", "Lsanna");

    final private static
    List<String> NAME_PREFIX_BY_Pe
        = List.of("Percival", "Perseus", "Penelope", "Peregrine", "Petrus");

    final private static
    List<String> TEST_PREFIX
        = List.of("Je", "Mi", "Alt", "Fe", "Lsa", "Pe");

    final private static
    List<String> TEST_GUILD_NAME
        = List.of(
            "Council of the Black Harvest",
            "The Dark Brotherhood", "Gank and Spank",
            "Ashenfall Vanguard", "Moonshadow Cartel",
            "Bloodoath Sentinels"
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

    /**
     * 将一个列表平均分成 n 个子列表（尽可能均匀分配）。
     * 如果无法整除，多余的元素会均匀分布在前几个子列表中。
     *
     * @param namesList 原始列表
     * @param splits    要分成的子列表数量
     *
     * @return          分割后的子列表集合
     *
     * @throws IllegalArgumentException 如果 splits <= 0 或列表无法分割时抛出
     */
    @Contract(pure = true)
    private @NotNull List<List<String>>
    splitAvgList(@NotNull List<String> namesList, int splits)
    {
        if (splits <= 0)
        {
            throw new IllegalArgumentException(
                "Param splits must be positive!"
            );
        }

        int size = namesList.size();
        if (size < splits)
        {
            throw new IllegalArgumentException(
                String.format("list size (%d) < splits (%d)", size, splits)
            );
        }

        List<List<String>> result = new ArrayList<>();
        int eachLen   = size / splits;      // 基础长度
        int remainder = size % splits;      // 剩余长度（需要额外分配）

        int start = 0;
        for (int i = 0; i < splits; i++)
        {
            int end = start + eachLen + ((i < remainder) ? 1 : 0);
            result.add(namesList.subList(start, end));
            start = end;
        }

        return result;
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

        List<String> uuidOfFePrefix
            = createUsersFromList(NAME_PREFIX_BY_Fe);

        List<String> uuidOfLsaPrefix
            = createUsersFromList(NAME_PREFIX_BY_Lsa);

        List<String> uuidOfPePrefix
            = createUsersFromList(NAME_PREFIX_BY_Pe);

        // 2. 令各个用户列表的第一个用户创建公会，并成为该公会的 Leader
        createGuildFromListHead(
            List.of(
                uuidsOfJePrefix.getFirst(),
                uuidsOfMiPrefix.getFirst(),
                uuidOfAltPrefix.getFirst(),
                uuidOfFePrefix.getFirst(),
                uuidOfLsaPrefix.getFirst(),
                uuidOfPePrefix.getFirst()
            ), TEST_GUILD_NAME
        );

        /* 合并各个用户列表除第一个用户之外的其他所有用户为一个新不变列表，然后打乱。*/
        List<String> mergedNames
            = this.mergeAndShuffleList(
                uuidsOfJePrefix.stream().skip(1L).toList(),
                uuidsOfMiPrefix.stream().skip(1L).toList(),
                uuidOfAltPrefix.stream().skip(1L).toList(),
                uuidOfFePrefix.stream().skip(1L).toList(),
                uuidOfLsaPrefix.stream().skip(1L).toList(),
                uuidOfPePrefix.stream().skip(1L).toList()
            );

        var results
            = this.splitAvgList(mergedNames, TEST_GUILD_NAME.size());

        for (int index = 0; index < TEST_GUILD_NAME.size(); ++index)
        {
            this.joinGuildFromListExpectHead(
                results.get(index),
                TEST_GUILD_NAME.get(index)
            );
        }

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

    /** 修剪掉字符串两端的双引号。*/
    private String
    trimStringHandle(@NotNull String string)
    {
        return
        string.contains("\"")
            ? string.replace("\"", "").trim()
            : string;
    }

    /** 测试 Redis Stream 类型的读取。*/
    @Order(4)
    @Test
    public void TestLogStreamRead()
    {
        final String userLogKey = "users:log";

        List<UserLogDTO> userOperatorLogs
            = this.stringRedisTemplate
            .opsForStream()
            .range(userLogKey, Range.open("-", "+"))
            .collectList()
            .map((records) ->
                records.stream()
                    .map((record) -> {
                        String recordId               = record.getId().getValue();
                        Map<Object, Object> recordVal = record.getValue();

                        return
                        new UserLogDTO()
                            .setRecordId(recordId)
                            .setEvent(trimStringHandle((String) recordVal.get("event")))
                            .setUuid(trimStringHandle((String) recordVal.get("uuid")))
                            .setUserName(trimStringHandle((String) recordVal.get("user-name")))
                            .setUserFunds(trimStringHandle((String) recordVal.get("user-funds")))
                            .setTimeStamp(trimStringHandle(String.valueOf(recordVal.get("timestamp"))));
                    }).toList()
            ).block();

        Assertions.assertNotNull(userOperatorLogs);
        userOperatorLogs.forEach(System.out::println);
    }

    /** 最后调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
    @Order(6)
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