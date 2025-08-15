package com.example.jesse.item_market.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.example.jesse.item_market.utils.UUIDGenerator.generateAsHex;

/** 项目 Redis Key 组合工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class KeyConcat
{
    /** 用户数据键前缀。*/
    public static final String USER_PREFIX      = "users:";

    /** 用户哈希的用户名字段名。*/
    public static final String USER_NAME_FIELD  = "name";

    /** 用户哈希的用户资金字段名。*/
    public static final String USER_FUNDS_FIELD = "funds";

    /** 用户哈希的用户公会字段名。*/
    public static final String USER_GUILD_FIELD = "guild";

    /** 用户哈希的用户公会身份字段名。*/
    public static final String USER_GUILD_ROLE_FIELD = "guild-role";

    /** 用户联系人数据键前缀。*/
    public static final String USER_CONTACT_PREFIX = "contact:";

    /** 用户包裹数据键前缀。*/
    public static final String INVENTORY_PREFIX = "inventories:";

    /** 武器市场数据键前缀。*/
    public static final String MARKET_PREFIX = "market:weapon-market:";

    /** 武器市场的价格表数据键。*/
    public static final String WEAPON_PRICE_ZSET = "market:weapon-market:weapon-price";

    /** 公会数据键前缀。*/
    public static final String GUILD_PREFIX = "guild:";

    /** 公会名数据键。*/
    public static final String GUILD_NAME_SET_KEY = "guild:guild-name:";

    /** Redis 锁键前缀。*/
    public static final String REDIS_LOCK_PREFIX = "lock:";

    /** 组合用户数据键。（示例：users:114940680399943670）*/
    @Contract(pure = true)
    public static @NotNull String
    getUserKey(String uuid) { return USER_PREFIX + uuid; }

    /** 组合用户最近联系人键。*/
    @Contract(pure = true)
    public static @NotNull String
    getContactKey(String uuid) { return USER_CONTACT_PREFIX + uuid; }

    /** 组合用户最近联系人日志键。*/
    @Contract(pure = true)
    public static @NotNull String
    getContactLogKey() { return USER_CONTACT_PREFIX + "log"; }

    /** 组合用户包裹键。（示例：inventories:114940680399943670）*/
    @Contract(pure = true)
    public static @NotNull String
    getInventoryKey(String uuid) {
        return INVENTORY_PREFIX + uuid;
    }

    /** 用户姓名与 UUID 哈希键。（user-name:user-name-hash）*/
    @Contract(pure = true)
    public static @NotNull String
    getUserHashKey() { return "user-name:user-name-hash"; }

    /**
     * 为即将上架至市场的武器生成一个 UUID。
     * （示例：market:weapon-market:weapons:1985f067af74d6d）
     */
    @Contract(pure = true)
    public static @NotNull String
    getNewWeaponHashKey() {
        return MARKET_PREFIX + "weapons:" + generateAsHex();
    }

    /**
     * 已知武器 ID，拼合对应的键。
     * （示例：market:weapon-market:weapons:1985f067af74d6d）
     */
    @Contract(pure = true)
    public static @NotNull String
    getWeaponHashKey(String weaponId) { return MARKET_PREFIX + "weapons:" + weaponId;}

    /** 存储上架武器价格的 Zset key（market:weapon-market:weapon-price） */
    @Contract(pure = true)
    public static @NotNull String
    getWeaponPriceZsetKey() {
        return WEAPON_PRICE_ZSET;
    }

    /** 组合公会键。（示例:guild:The-Dark-Brotherhood） */
    @Contract(pure = true)
    public static @NotNull String
    getGuildKey(String guildName) { return GUILD_PREFIX + guildName; }

    /** 获取公会名数据键。*/
    @Contract(pure = true)
    public static @NotNull String
    getGuildNameSetKey() { return GUILD_NAME_SET_KEY + "guild-name-set"; }

    /** 组合公会操作日志键。*/
    @Contract(pure = true)
    public static @NotNull String
    getGuildLogKey() { return GUILD_PREFIX + "log"; }

    /** 获取公会名集合日志键。*/
    @Contract(pure = true)
    public static @NotNull String
    getGuildNameSetLogKey() { return GUILD_NAME_SET_KEY + "log"; }

    /** 组合 Redis 锁键。*/
    @Contract(pure = true)
    public static @NotNull String
    getRedisLockKey(String keyName) { return REDIS_LOCK_PREFIX + keyName; }
}