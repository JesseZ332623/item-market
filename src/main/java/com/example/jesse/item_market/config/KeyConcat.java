package com.example.jesse.item_market.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.example.jesse.item_market.utils.UUIDGenerator.generateAsHex;

/** 项目 Redis Key 组合工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class KeyConcat
{
    public static final String USER_PREFIX      = "users:";
    public static final String USER_NAME_FIELD  = "name";
    public static final String USER_FUNDS_FIELD = "funds";

    public static final String INVENTORY_PREFIX = "inventories:";

    public static final String MARKET_PREFIX     = "market:weapon-market:";
    public static final String WEAPON_PRICE_ZSET = "weapon-price";

    /** 组合用户数据键（示例：users:114940680399943670）*/
    @Contract(pure = true)
    public static @NotNull String
    getUserKey(String uuid) {
        return USER_PREFIX + uuid;
    }

    /** 组合用户包裹键（示例：inventories:114940680399943670）*/
    @Contract(pure = true)
    public static @NotNull String
    getInventoryKey(String uuid) {
        return INVENTORY_PREFIX + uuid;
    }

    /** 用户集合键（user-name:user-name-set）。*/
    @Contract(pure = true)
    public static @NotNull String
    getUserSetKey() { return "user-name:user-name-set"; }

    /**
     * 为即将上架至市场的武器生成一个 UUID
     * （示例：market:weapon-market:weapons:1985f067af74d6d）
     */
    @Contract(pure = true)
    public static @NotNull String
    getWeaponHashKey() {
        return MARKET_PREFIX + "weapons:" + generateAsHex();
    }

    /** 存储上架武器价格的 Zset key（market:weapon-market:weapon-price） */
    @Contract(pure = true)
    public static @NotNull String
    getWeaponPriceZsetKey() {
        return MARKET_PREFIX + WEAPON_PRICE_ZSET;
    }
}
