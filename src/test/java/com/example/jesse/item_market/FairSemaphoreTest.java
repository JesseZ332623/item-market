package com.example.jesse.item_market;

import com.example.jesse.item_market.semaphore.FairSemaphore;
import com.example.jesse.item_market.user.UserRedisService;
import com.example.jesse.item_market.user.Weapons;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.jesse.item_market.utils.LimitRandomElement.getRandomLimit;

/** Redis 公平信号量测试。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FairSemaphoreTest
{
    private final static List<String> TEST_USERS
        = List.of(
        "Jesse", "Mike", "Frank", "Tom", "Peter",
        "Lisa", "John", "Jackson", "Lois", "Jesus",
        "Silly", "Lister", "Franklin", "Jerry", "Mask",
        "Steve", "Jem", "Meg", "Cris", "Evan", "Jean",
        "Stubbies", "Bill", "Billy", "Kiki"
    );

    private final static List<Weapons> TEST_WEAPONS
        = Arrays.asList(Weapons.values());

    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FairSemaphore fairSemaphore;

    @Autowired
    private UserRedisService userRedisService;

    /** 初始化一些用户，和他们的包裹。*/
    private void CreateSomeNewUsers()
    {
        log.info("Call Method: CreateSomeNewUsers()");

        Flux.fromIterable(TEST_USERS)
            .flatMap((name) ->
                this.userRedisService.addNewUser(name))
            .blockLast();
    }

    /** 为所有用户入库 10 个随机武器。*/
    private void AddWeaponToInventory()
    {
        log.info("Call Method: AddWeaponToInventory()");

        this.userRedisService
            .getAllUserUUID().collectList()
            .flatMapMany(Flux::fromIterable)
            .flatMap((uuid) ->
                Flux.fromIterable(getRandomLimit(TEST_WEAPONS, 10L))
                    .flatMap((weapon) ->
                        this.userRedisService
                            .addWeaponToInventory(uuid, weapon))
                    .then()
            ).blockLast();
    }

    /**
     * 将所有用户的随机几个装备上架市场售卖。
     */
    private void AddWeaponToMarket()
    {
        log.info("Call Method: AddWeaponToMarket()");

        // 1. 对每个用户，随机选 5 个武器上架市场
        this.userRedisService
            .getAllUserUUID()
            .flatMap(uuid ->
                this.userRedisService.getAllWeaponsFromInventoryByUUID(uuid)
                    .collectList() // 先收集所有武器
                    .flatMapMany(weaponsList ->
                        Mono.defer(() -> {
                            List<Weapons> randomWeapons
                                = getRandomLimit(new ArrayList<>(weaponsList), 5L);

                            double randomValue
                                = ThreadLocalRandom
                                    .current()
                                    .nextDouble(15.00, 35.00);

                            return Flux.fromIterable(randomWeapons)
                                .flatMap(weapon ->
                                    this.userRedisService.addWeaponToMarket(
                                        uuid, weapon,
                                        // 价格需要保留 2 位小数
                                        Math.round(randomValue * 100) / 100.00
                                    )
                                ).then();
                        })
                    )
            ).blockLast(); // 阻塞直到所有操作完成
    }

    private String GetRandomUserId()
    {
        log.info("Call Method: GetRandomUserId()");

        List<String> uuids
            = this.userRedisService
                 .getAllUserUUID()
                 .collectList()
                 .block();

        Assertions.assertNotNull(uuids);

        return uuids.get(
            ThreadLocalRandom
                .current()
                .nextInt(0, uuids.size())
        );
    }

    /** 设置信号量为 10，模拟有 10 个用户观察某一个用户上架至市场的武器。*/
    @Test
    @Order(1)
    public void TestFairSemaphore()
    {
        final String semaphoreName = "semaphore:remote";

        CreateSomeNewUsers();
        AddWeaponToInventory();
        AddWeaponToMarket();

        final String observedUserId = GetRandomUserId();

        Flux.range(0, 10)
            .flatMap((index) ->
                this.fairSemaphore
                    .withFairSemaphore(
                        semaphoreName,
                        10L, 10L,
                        (identifier) ->
                            this.userRedisService
                                .getAllWeaponsFromMarketByUUID(observedUserId)
                                .collectList()
                                .doOnSuccess(System.out::println)
                    )
            ).blockLast();
    }

    /** 最后调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
    @Test
    @Order(2)
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
