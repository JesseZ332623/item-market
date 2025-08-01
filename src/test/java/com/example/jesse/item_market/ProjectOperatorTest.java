package com.example.jesse.item_market;

import com.example.jesse.item_market.market.MarketService;
import com.example.jesse.item_market.user.impl.UserRedisServiceImpl;
import com.example.jesse.item_market.user.Weapons;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.jesse.item_market.utils.LimitRandomElement.getRandomLimit;
import static com.example.jesse.item_market.utils.TestUtils.SELECT_AMOUNT;

/** 项目 Redis 操作测试。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectOperatorTest
{
    @Autowired
    private UserRedisServiceImpl userRedisService;

    @Autowired
    private MarketService marketService;

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

    /** 为所有用户入库 10 个随机武器。*/
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
                                .nextDouble(15.00, 1250.00);

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

    /** 获取每一个用户在市场山出售的武器列表。*/
    @Order(6)
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
    @Order(6)
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

//    @Order(7)
//    @Test
//    void TestMarketTransaction()
//    {
//        // 1. SELECT_AMOUNT 个随机挑选的用户中，前半用户作为买家，后半部分作为卖家
//        Mono<Tuple2<List<String>, List<String>>> splitUsers
//            = this.userRedisService.getAllUserUUID()
//            .collectList()
//            .map(uuidList ->
//                getRandomLimit(uuidList, SELECT_AMOUNT))
//            .map((users) -> {
//                int half = users.size() / 2;
//
//                return Tuples.of(
//                    users.subList(0, half),
//                    users.subList(half, users.size())
//                );
//            });
//
//        // 3. 后半用户作为卖家，为每一个卖家挑一个在他们包裹中的武器并上架至市场
//        Mono<Map<String, Weapons>> sellerWeaponsMap
//            = splitUsers.flatMapMany(
//                (tuple) ->
//                    Flux.fromIterable(tuple.getT2()))
//            .concatMap((uuid) ->
//                this.userRedisService
//                    .getAllWeaponsByUUID(uuid)
//                    .map(Weapons::valueOf)
//                    .collectList()
//                    .flatMap((weapons) -> {
//                            ThreadLocalRandom random = ThreadLocalRandom.current();
//
//                            Weapons weaponSelect
//                                = weapons.get(random.nextInt(0, weapons.size()));
//
//                            double randomValue
//                                = random.nextDouble(15.00, 650.00);
//
//                            System.out.printf(
//                                "Inbound weapon: %s, price: %.2f\n",
//                                weaponSelect, randomValue
//                            );
//
//                            return this.userRedisService
//                                .addWeaponToMarket(
//                                    uuid, weaponSelect,
//                                    Math.round(randomValue * 100) / 100.00)
//                                .then(Mono.just(Tuples.of(uuid, weaponSelect)));
//                        }
//                    )
//            ).collectMap(Tuple2::getT1, Tuple2::getT2);
//
//        // 4. 执行交易
//        splitUsers.zipWith(sellerWeaponsMap)
//            .flatMapMany(tuple -> {
//                Tuple2<List<String>, List<String>> users = tuple.getT1();
//                Map<String, Weapons> weaponsMap          = tuple.getT2();
//
//                List<String> buyers  = users.getT1();
//                List<String> sellers = users.getT2();
//
//                return Flux.fromIterable(buyers)
//                    .concatMap(buyerId ->
//                        Flux.fromIterable(sellers)
//                            .concatMap(sellerId -> {
//                                Weapons weapon = weaponsMap.get(sellerId);
//
//                                return this.marketService
//                                    .marketTransaction(buyerId, sellerId, weapon);
//                            })
//                    );
//            }).blockLast();
//    }

    /** 删除随机挑选的 5 个用户。*/
    @Order(8)
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
}
