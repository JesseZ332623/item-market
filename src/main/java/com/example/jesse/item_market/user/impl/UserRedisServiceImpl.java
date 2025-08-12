package com.example.jesse.item_market.user.impl;

import com.example.jesse.item_market.user.UserRedisService;
import com.example.jesse.item_market.user.Weapons;
import com.example.jesse.item_market.user.dto.UserInfo;
import com.example.jesse.item_market.utils.LimitRandomElement;
import com.example.jesse.item_market.utils.LuaScriptReader;
import com.example.jesse.item_market.utils.dto.LuaOperatorResult;
import com.example.jesse.item_market.errorhandle.ProjectRedisOperatorException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.example.jesse.item_market.utils.KeyConcat.*;
import static com.example.jesse.item_market.errorhandle.RedisErrorHandle.redisGenericErrorHandel;
import static com.example.jesse.item_market.utils.UUIDGenerator.generateAsSting;
import static java.lang.String.format;
import static com.example.jesse.item_market.utils.LuaScriptOperatorType.USER_OPERATOR;

/** 用户 Redis 操作实现类。*/
@Slf4j
@Service
public class UserRedisServiceImpl implements UserRedisService
{
    /** 十八般武器列表。*/
    private final static List<Weapons> WEAPONS
        = Arrays.asList(Weapons.values());

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

    /** 获取所有用户的 UUID。*/
    @Override
    public Flux<String> getAllUserUUID()
    {
        return this.redisTemplate
            .scan(
                ScanOptions.scanOptions()
                    .match("users:[0-9]*")
                    .build())
            .timeout(Duration.ofSeconds(5L))
            .switchIfEmpty(Mono.empty())
            .map((matchedUUID) -> matchedUUID.split(":")[1].trim())
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null));
    }

    /** 获取某个用户的最近联系人列表。*/
    @Override
    public Flux<String>
    getContactListByUUID(String uuid)
    {
        return
        this.redisTemplate.opsForList()
            .range(getContactKey(uuid), 0L, -1L)
            .map((name) -> (String) name);
    }

    /**
     * 当用户在前端页面的最近联系人搜索框中输入若干字母时，
     * 匹配该用户最近联系人列表中所有 prefix 开头的联系人名。
     * 由于最近联系人列表长度有限（不超过 175），所以效率不会慢。
     *
     * @param uuid   搜索哪个用户的最近联系人列表？
     * @param prefix 用户在搜索框输入了什么？
     *
     * @return 发布匹配该用户最近联系人列表中所有 prefix 开头的联系人名数据的 Mono
     */
    @Override
    public Flux<String>
    fetchAutoCompleteContact(String uuid, String prefix)
    {
        return
        this.redisTemplate
            .opsForList()
            .range(getContactKey(uuid), 0 , -1)
            .timeout(Duration.ofSeconds(5L))
            .map((res) -> ((String) res).toLowerCase(Locale.ROOT))
            .filter((contact) ->
                (contact.startsWith(prefix.toLowerCase(Locale.ROOT))))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null));
    }

    @Override
    public Flux<Weapons>
    getAllWeaponsFromInventoryByUUID(String uuid)
    {
        return this.redisTemplate.opsForList()
            .range(getInventoryKey(uuid), 0L, -1L)
            .timeout(Duration.ofSeconds(5L))
            .switchIfEmpty(
                Mono.error(
                    new ProjectRedisOperatorException(
                        format("User: %s weapons not found!", uuid), null
                    )
                )
            )
            .map((weapon) -> Weapons.valueOf((String) weapon))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null));
    }

    /** 获取某个用户上架至市场的所有武器。*/
    @Override
    public Flux<Weapons>
    getAllWeaponsFromMarketByUUID(String uuid)
    {
        return
        this.redisTemplate
            .scan(
                ScanOptions.scanOptions()
                    .match("market:weapon-market:weapons:*")
                    .build())
            .flatMap((key) ->
                this.redisTemplate.opsForHash()
                    .multiGet(key, List.of("\"weapon-name\"", "\"seller\""))
                    .filter((result) -> result.get(1).equals(uuid))
                    .map((res) -> Weapons.valueOf((String) res.getFirst()))
            );
    }

    @Override
    public Mono<UserInfo>
    getUserInfoByUUID(String uuid)
    {
        return
        this.redisTemplate.opsForHash()
            .multiGet(getUserKey(uuid), List.of("\"name\"", "\"funds\""))
            .map((result) ->
                new UserInfo()
                    .setUserName((String) result.getFirst())
                    .setUserFunds((double) result.get(1))
            );
    }

    /** 获取某个用户上架至市场的所有武器的 ID。*/
    @Override
    public Flux<String>
    getAllWeaponIdsFromMarketByUUID(String uuid)
    {
        return
        this.redisTemplate
            .scan(
                ScanOptions.scanOptions()
                    .match("market:weapon-market:weapons:*")
                    .build())
            .flatMap((key) ->
                this.redisTemplate.opsForHash()
                    .get(key, "\"seller\"")
                    .filter(sellerId -> sellerId.equals(uuid))
                    .map(ignore ->
                        key.substring(key.lastIndexOf(":") + 1))
            );
    }

    /**
     * 创建一个新用户，并为它随机挑选几件武器放入包裹，
     * 分为以下几个操作：
     *
     * <ol>
     *     <li>尝试往用户集合内插入新用户，确保用户名不重复</li>
     *     <li>添加新用户</li>
     *     <li>随机挑选几件武器放入它的包裹</li>
     * </ol>
     *
     * <strong>
     *     需要注意的是，本方法的 uuid 和 weaponsString 采用随机值，
     *     故需要使用 Mono.defer() 来包装整体操作确保随机。
     * </strong>
     *
     * @param userName 新用户名
     *
     * @return 发布新用户 UUID 的 Mono
     */
    @Override
    public @NotNull Mono<String>
    addNewUser(String userName)
    {
        return Mono.defer(() -> {
            final String uuid          = generateAsSting();
            final String userKey       = getUserKey(uuid);
            final String userSetKey    = getUserSetKey();
            final String inventoryKey  = getInventoryKey(uuid);
            final float NEW_USER_FUNDS = 12500.00F;
            final String weaponsString
                = LimitRandomElement.getRandomLimit(WEAPONS, 6)
                        .stream()
                        .map(Weapons::getItemName)
                        .collect(Collectors.joining(" "));

                return this.luaScriptReader
                    .fromFile(USER_OPERATOR, "addNewUser.lua")
                    .flatMap((script) ->
                        this.redisScriptTemplate
                            .execute(
                                script,
                                List.of(userKey, userSetKey, inventoryKey),
                                USER_NAME_FIELD,
                                USER_FUNDS_FIELD,
                                USER_GUILD_FIELD,
                                USER_GUILD_ROLE_FIELD,
                                userName,
                                NEW_USER_FUNDS,
                                weaponsString)
                            .timeout(Duration.ofSeconds(5L))
                            .next()
                            .flatMap((result) -> {
                                if ("DUPLICATE_USER".equals(result.getResult()))
                                {
                                    return Mono.error(
                                        new IllegalArgumentException(
                                            format("User: %s is exist!", userName)
                                        )
                                    );
                                }
                                else if ("SUCCESS".equals(result.getResult())) {
                                    return Mono.just(uuid);
                                }
                                else
                                {
                                    return Mono.error(
                                        new IllegalStateException(
                                            "Unexpected result: " + result.getResult()
                                        )
                                    );
                                }
                            })
                            .onErrorResume((exception) ->
                                redisGenericErrorHandel(exception, null)
                            )
                    );
        });
    }

    /**
     * 用户记录另一个用户为最近联系人，分为以下几个操作：
     *
     * <ol>
     *     <li>检查要添加的用户十是否存在与列表中，如果存在要移除</li>
     *     <li>往列表中添加指定用户</li>
     *     <li>倘若列表长度超过上限（假设是 100 个），则移除列表末尾的联系人</li>
     * </ol>
     *
     * @param uuid         哪个用户要添加一条最近联系人？
     * @param contactName  哪个用户成为了它的最近联系人？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    addNewContact(String uuid, String contactName)
    {
        final String userKey       = getUserKey(uuid);
        final String contactKey    = getContactKey(uuid);
        final String contactLogKey = getContactLogKey();
        final int USER_MAX_CONTACT = 10;

        return
        this.luaScriptReader
            .fromFile(USER_OPERATOR, "addNewContact.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script, List.of(userKey, contactKey, contactLogKey),
                        uuid, contactName, USER_MAX_CONTACT)
                    .next()
                    .timeout(Duration.ofSeconds(3L))
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "SELF_ADDED" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        "Self added is forbidden!"
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        }
                    )
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 用户删除自己最近联系人列表的某个用户。
     *
     * @param uuid         哪个用户要删除一条最近联系人？
     * @param contactName  要删除哪个用户？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    removeContact(String uuid, String contactName)
    {
        final String contactKey    = getContactKey(uuid);
        final String contactLogKey = getContactLogKey();

        return
        this.luaScriptReader
            .fromFile(USER_OPERATOR, "removeContact.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(contactKey, contactLogKey),
                        uuid, contactName)
                    .next()
                    .timeout(Duration.ofSeconds(3L))
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "CONCAT_NAME_NOT_FOUND" ->
                                Mono.error(
                                    new IllegalArgumentException(
                                        format("Concat name: %s not found!", contactName)
                                    )
                                );

                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        }
                    )
            )
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 为用户的包裹添加一件武器。
     *
     * @param uuid   用户的 uuid
     * @param weapon 武器类型
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    addWeaponToInventory(String uuid, @NotNull Weapons weapon)
    {
        return this.luaScriptReader
            .fromFile(USER_OPERATOR, "addWeaponToInventory.lua")
            .flatMap((script) ->
                this.redisScriptTemplate.execute(
                        script, List.of(getUserKey(uuid), getInventoryKey(uuid)), weapon.getItemName())
                    .next()
                    .timeout(Duration.ofSeconds(5L))
                    .onErrorResume((exception) ->
                        redisGenericErrorHandel(exception, null))
                    .then()
            );
    }

    /**
     * 用户销毁包裹中的某个武器。
     *
     * @param uuid   用户的 uuid
     * @param weapon 武器类型
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    destroyWeaponFromInventory(String uuid, @NotNull Weapons weapon)
    {
        final String inventoryKey = getInventoryKey(uuid);
        final String userKey      = getUserKey(uuid);

        return
        this.luaScriptReader
            .fromFile(USER_OPERATOR, "destroyWeaponFromInventory.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(inventoryKey, userKey),
                        uuid, weapon.getItemName())
                    .next()
                    .timeout(Duration.ofSeconds(5L))
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        }
                    ))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 用户将武器上架至市场，分为以下几个操作：
     *
     * <ol>
     *     <li>移除用户包裹中的对应武器</li>
     *     <li>以生成的武器 uid 为键，使用哈希表存储武器名和卖家 uuid 两个字段</li>
     *     <li>以生成的武器 uid 为成员，武器价格为分数（使用有序列表）</li>
     * </ol>
     *
     * @param uuid   用户的 uuid
     * @param weapon 武器类型
     * @param price  销售价格
     *
     * @return 发布上架至市场的武器 UID 的 Mono
     */
    @Override
    public Mono<String>
    addWeaponToMarket(String uuid, @NotNull Weapons weapon, double price)
    {
        return Mono.defer(() -> {
            final String weaponKey      = getNewWeaponHashKey();
            final String weaponPriceKey = getWeaponPriceZsetKey();
            final String inventoryKey   = getInventoryKey(uuid);
            final String userKey        = getUserKey(uuid);

            final String weaponUUID
                = weaponKey.substring(weaponKey.lastIndexOf(":") + 1);

            return this.luaScriptReader
                .fromFile(USER_OPERATOR, "addWeaponToMarket.lua")
                .flatMap((script) ->
                    this.redisScriptTemplate
                        .execute(
                            script,
                            List.of(weaponKey, weaponPriceKey, inventoryKey, userKey),
                            weaponUUID, uuid, weapon.getItemName(), price)
                        .timeout(Duration.ofSeconds(5L))
                        .next()
                        .flatMap((result) ->
                            switch (result.getResult()) {
                                case "INVENTORY_REM_FAILED" ->
                                    Mono.error(
                                        new IllegalStateException(
                                            format(
                                                "Weapon: %s not exist in User: %s 's inventory.",
                                                weapon.getItemName(), uuid
                                            )
                                        )
                                    );

                                case "SUCCESS" -> Mono.just(weaponUUID);

                                case null, default ->
                                    Mono.error(
                                        new IllegalStateException(
                                            "Unexpected result: " + result.getResult()
                                        )
                                    );
                            })
                        .onErrorResume((exception) ->
                            redisGenericErrorHandel(exception, null))
                );
        });
    }

    /**
     * 用户从市场上下架某个武器，分为以下几个操作。
     *
     * <ol>
     *     <li>移除对应用户的武器在市场中的数据</li>
     *     <li>重新将武器放回对应用户的包裹中</li>
     * </ol>
     *
     * @param uuid   用户的 uuid
     * @param weapon 武器类型
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    removeWeaponFromMarket(String uuid, @NotNull Weapons weapon)
    {
        final String userKey        = getUserKey(uuid);
        final String weaponPriceKey = getWeaponPriceZsetKey();
        final String inventoryKey   = getInventoryKey(uuid);

        return
        this.luaScriptReader
            .fromFile(USER_OPERATOR, "removeWeaponFromMarket.lua")
            .flatMap((script) ->
                this.redisScriptTemplate
                    .execute(
                        script,
                        List.of(userKey, weaponPriceKey, inventoryKey),
                        uuid, weapon.getItemName())
                    .next()
                    .timeout(Duration.ofSeconds(5L))
                    .flatMap((result) ->
                        switch (result.getResult())
                        {
                            case "SUCCESS" -> Mono.empty();

                            case null, default ->
                                Mono.error(
                                    new IllegalStateException(
                                        "Unexpected result: " + result.getResult()
                                    )
                                );
                        }
                    ))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .then();
    }

    /**
     * 删除用户，分为以下几个操作：
     *
     * <ol>
     *     <li>删除用户集合上的用户名</li>
     *     <li>删除用户挂在市场上的所有物品</li>
     *     <li>删除用户对应的包裹数据</li>
     *     <li>删除用户数据</li>
     * </ol>
     *
     * <strong>上述操作会放在 Lua 脚本里面执行，确保原子性。</strong>
     *
     * @param uuid 用户的 uuid
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void> deleteUser(String uuid)
    {
        final String userKey      = getUserKey(uuid);
        final String userSetKey   = getUserSetKey();
        final String inventoryKey = getInventoryKey(uuid);

        return this.luaScriptReader
                   .fromFile(USER_OPERATOR, "deleteUser.lua")
                   .flatMap((script) ->
                       this.redisScriptTemplate.execute(
                           script,
                           List.of(userKey, userSetKey, inventoryKey),
                           USER_NAME_FIELD, USER_FUNDS_FIELD)
                       .timeout(Duration.ofSeconds(5L))
                       .next()
                       .flatMap((result) ->
                           switch (result.getResult())
                           {
                               case "USER_NOT_FOUND" ->
                                   Mono.error(
                                       new IllegalArgumentException(
                                           format("UUID: %s not exits!", uuid)
                                       )
                               );

                               case "INVALID_USER_KEY" ->
                                   Mono.error(
                                       new IllegalArgumentException(
                                           format("Failed to extract UUID from %s.", userKey)
                                   )
                               );

                               case "SUCCESS" -> Mono.empty();

                               case null, default -> Mono.error(
                                   new IllegalStateException(
                                       "Unexpected result: " + result.getResult()
                                   )
                               );
                       })
                       .onErrorResume((exception) ->
                           redisGenericErrorHandel(exception, null))
                       .then()
                   );
    }
}
