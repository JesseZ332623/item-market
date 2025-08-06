package com.example.jesse.item_market;

import com.example.jesse.item_market.guild.GuildRedisService;
import com.example.jesse.item_market.market.MarketService;
import com.example.jesse.item_market.user.UserRedisService;
import com.example.jesse.item_market.user.Weapons;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.jesse.item_market.utils.LimitRandomElement.getRandomLimit;
import static com.example.jesse.item_market.utils.TestUtils.SELECT_AMOUNT;

/** 项目 Redis 操作测试。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectOperatorTest
{
    @Autowired
    private
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private MarketService marketService;

    @Autowired
    private GuildRedisService guildRedisService;

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

    /** 初始化一些用户，和他们的包裹。*/
    @Order(1)
    @Test
    public void TestCreateSomeNewUsers()
    {
        Flux.fromIterable(TEST_USERS)
            .flatMap((name) ->
                this.userRedisService
                    .addNewUser(name))
            .blockLast();
    }

    /**
     * 对于 TEST_USERS 中的每一个用户，
     * 将用户列表中处自己之外的用户全部保存为最近联系人。
     */
    @Order(2)
    @Test
    public void TestAddNewContact()
    {
        this.userRedisService
            .getAllUserUUID()
            .flatMap((uuid) ->
                this.userRedisService
                    .getUserInfoByUUID(uuid)
                    .flatMap((userInfo) -> {
                        List<String> listWithoutSelf
                            = TEST_USERS.stream()
                                .filter(
                                    (userName) ->
                                        !userName.equals(userInfo.getUserName()))
                                .toList();

                        return
                        Flux.fromIterable(listWithoutSelf)
                            .flatMap((userName) ->
                                this.userRedisService
                                    .addNewContact(uuid, userName))
                            .then();
                    })
            ).blockLast();
    }

    /** 随机挑选几个用户删除他们最近联系人列表的某两个用户。*/
    @Order(3)
    @Test
    public void TestRemoveContact()
    {
        this.userRedisService
            .getAllUserUUID().collectList()
            .map((uuids) ->
                getRandomLimit(uuids, 5L))
            .flatMap((selectedIds) ->
                Flux.fromIterable(selectedIds)
                    .flatMap((uuid) ->
                        this.userRedisService
                            .getContactListByUUID(uuid)
                            .collectList()
                            .map((contacts) ->
                                getRandomLimit(contacts, 2L))
                            .flatMap((selectedContacts) ->
                                Flux.fromIterable(selectedContacts)
                                    .flatMap((contact) ->
                                        this.userRedisService.removeContact(uuid, contact))
                                    .then()
                            ))
                    .then()
            ).block();
    }

    /** 为所有用户入库 10 个随机武器。*/
    @Order(4)
    @Test
    public void TestAddWeaponToInventory()
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

    /** 每一个用户销毁自己包裹中的两件随机武器。*/
    @Order(5)
    @Test
    public void TestDestroyWeaponFromInventory()
    {
        this.userRedisService
            .getAllUserUUID().collectList()
            .flatMapMany(Flux::fromIterable)
            .flatMap((uuid) ->
                this.userRedisService
                    .getAllWeaponsFromInventoryByUUID(uuid)
                    .collectList()
                    .map((weapons) ->
                        getRandomLimit(weapons, 2L))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap((weapon) ->
                        this.userRedisService
                            .destroyWeaponFromInventory(uuid, weapon)
                    )
            ).blockLast();
    }

    /**
     * 随机取几个用户，
     * 将他们的随机几个装备上架市场售卖。
     */
    @Order(6)
    @Test
    public void TestAddWeaponToMarket()
    {
        // 1. 获取随机用户列表
        Mono<List<String>> randomUsers
            = this.userRedisService.getAllUserUUID()
                  .collectList()
                  .map(uuidList -> getRandomLimit(uuidList, SELECT_AMOUNT));

        // 2. 对每个用户，随机选 2 个武器上架市场
        randomUsers.flatMapMany(Flux::fromIterable)
            .flatMap(uuid ->
                this.userRedisService.getAllWeaponsFromInventoryByUUID(uuid)
                    .collectList() // 先收集所有武器
                    .flatMapMany(weaponsList -> {
                       // log.info("User: {} Weapons List = {}", uuid, weaponsList);

                        return Mono.defer(() -> {
                            // 5 个武器
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
                        });
                    })
            ).blockLast(); // 阻塞直到所有操作完成
    }

    /** 获取每一个用户在市场出售的武器列表。*/
    @Order(7)
    @Test
    public void TestGetAllWeaponsFromMarketByUUID()
    {
        this.userRedisService.getAllUserUUID()
            .flatMap(uuid ->
                this.userRedisService
                    .getAllWeaponsFromMarketByUUID(uuid)
                    .collectList()
                    .doOnSuccess(System.out::println)
            ).blockLast();
    }

    /** 将每一个用户上架在市场（如果有的话）的其中 2 件武器下架，放回用户自己的包裹。*/
    @Order(8)
    @Test
    public void TestRemoveWeaponFromMarket()
    {
        this.userRedisService
            .getAllUserUUID().collectList()
            .flatMapMany(Flux::fromIterable)
            .flatMap((uuid) ->
                this.userRedisService
                    .getAllWeaponsFromMarketByUUID(uuid)
                    .collectList()
                    .filter((weapons) -> !weapons.isEmpty())
                    .map((weapons) -> getRandomLimit(weapons, 2L))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap((weapon) ->
                        this.userRedisService
                            .removeWeaponFromMarket(uuid, weapon)
                    )
            ).blockLast();
    }

    /** 市场交易测试。*/
    @Order(9)
    @Test
    void TestMarketTransaction()
    {
        // 1. SELECT_AMOUNT 个随机挑选的用户中，前半用户作为买家，后半部分作为卖家
        Mono<Tuple2<List<String>, List<String>>> splitUsers
            = this.userRedisService.getAllUserUUID()
            .collectList()
            .map(uuidList ->
                getRandomLimit(uuidList, SELECT_AMOUNT))
            .map((users) -> {
                int half = users.size() / 2;

                return Tuples.of(
                    users.subList(0, half),
                    users.subList(half, users.size())
                );
            });

        // 2. 搜索卖家在市场上上架的武器，并随机挑出一个，令买家与之交易
        splitUsers
            .flatMapMany(usersTuple -> {
                List<String> sellers = usersTuple.getT2();
                List<String> buyers  = usersTuple.getT1();

                return
                Flux.fromIterable(sellers)
                    .flatMap(seller ->
                        Flux.fromIterable(buyers)
                            .flatMap(buyer ->
                                this.userRedisService
                                .getAllWeaponIdsFromMarketByUUID(seller)
                                .collectList()
                                .filter(weaponList -> !weaponList.isEmpty())
                                .flatMap(weaponIds -> {
                                    String weaponId = weaponIds.get(
                                        ThreadLocalRandom.current().nextInt(weaponIds.size()));

                                    log.info(
                                        "Buyer: {}, Seller: {}, Weapon ID: {}",
                                        buyer, seller, weaponId
                                    );

                                    return this.marketService
                                               .marketTransaction(buyer, seller, weaponId);
                                })
                            )
                    );
            }).blockLast(); // 使用 blockLast() 等待所有交易完成
    }

    @Order(10)
    @Test
    public void TestCreateGuild()
    {
        final List<String> testGuildName
            = List.of(
                "Council of the Black Harvest",
                "The Dark Brotherhood",
                "Gank and Spank",
                "Method",
                "C'est La Vie"
        );

        Mono<Map<String, String>> testData
            = this.userRedisService
                  .getAllUserUUID()
                  .collectList()
                  .map((uuids) -> {
                      List<String> testUser = getRandomLimit(uuids, testGuildName.size());

                      return
                      IntStream.range(0, testUser.size())
                               .boxed()
                               .collect(Collectors.toMap(testUser::get, testGuildName::get));
                  });

        testData.map((dat) -> dat.entrySet().stream())
                .flatMap((entryStream) ->
                    Flux.fromStream(entryStream)
                        .flatMap((entry) ->
                            this.guildRedisService.createGuild(entry.getKey(), entry.getValue()))
                        .then()
            ).block();
    }

    /** 删除随机挑选的 5 个用户。*/
    @Order(11)
    @Test
    public void TestDeleteUser()
    {
        this.userRedisService.getAllUserUUID()
            .collectList()
            .map((uuids) -> getRandomLimit(uuids, 5L))
            .flatMapMany(Flux::fromIterable)
                   .flatMap((uuid) ->
                       this.userRedisService.deleteUser(uuid))
            .blockLast();
    }

    /** 最后调用 FLUSHALL ASYNC 命令，清空整个 Redis。*/
    @Order(12)
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
