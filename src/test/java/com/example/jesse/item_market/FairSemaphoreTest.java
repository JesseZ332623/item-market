package com.example.jesse.item_market;

import com.example.jesse.item_market.semaphore.FairSemaphore;
import com.example.jesse.item_market.user.UserRedisService;
import com.example.jesse.item_market.user.Weapons;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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
import static com.example.jesse.item_market.utils.TestUtils.SELECT_AMOUNT;

/** Redis 公平信号量测试。*/
@SpringBootTest
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
        Flux.fromIterable(TEST_USERS)
            .flatMap((name) ->
                this.userRedisService
                    .addNewUser(name))
            .blockLast();
    }

    /** 为所有用户入库 10 个随机武器。*/
    private void AddWeaponToInventory()
    {
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
     * 随机取几个用户，
     * 将他们的随机几个装备上架市场售卖。
     */
    private void AddWeaponToMarket()
    {
        // 1. 获取随机用户列表
        Mono<List<String>> randomUsers
            = this.userRedisService.getAllUserUUID()
            .collectList()
            .map(uuidList -> getRandomLimit(uuidList, SELECT_AMOUNT));

        // 2. 对每个用户，随机选 5 个武器上架市场
        randomUsers.flatMapMany(Flux::fromIterable)
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

    private String getRandomUserId()
    {
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

        final String observedUserId = getRandomUserId();

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
    @Order(2)
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
