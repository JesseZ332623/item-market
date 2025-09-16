package com.example.jesse.item_market.user.route;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** 用户服务路径配置类 - RESTful风格。 */
public class UserServiceURIConfig
{
    /** API 根路径 */
    private static final String API_ROOT = "/api";

    /** 用户资源 */
    public static final String USERS = API_ROOT + "/users";

    /** 特定用户 */
    @Contract(pure = true)
    public static @NotNull String user(String userId) {
        return USERS + "/{userId}";
    }

    /** 用户 UUID 列表 */
    public static final String USER_UUIDS = USERS + "/uuids";

    /** 用户联系人 */
    @Contract(pure = true)
    public static @NotNull String
    userContacts(String userId) {
        return user(userId) + "/contacts";
    }

    /** 特定联系人 */
    @Contract(pure = true)
    public static @NotNull String
    userContact(String userId, String contactName) {
        return userContacts(userId) + "/{contactName}";
    }

    /** 用户库存 */
    @Contract(pure = true)
    public static @NotNull String
    userInventory(String userId) {
        return user(userId) + "/inventory";
    }

    /** 用户库存武器 */
    @Contract(pure = true)
    public static @NotNull String
    userInventoryWeapons(String userId) {
        return userInventory(userId) + "/weapons";
    }

    /** 特定库存武器 */
    @Contract(pure = true)
    public static @NotNull String
    userInventoryWeapon(String userId, String weaponName) {
        return userInventoryWeapons(userId) + "/{weaponName}";
    }

    /** 用户市场列表 */
    @Contract(pure = true)
    public static @NotNull String
    userMarketListings(String userId) {
        return user(userId) + "/market-listings";
    }

    /** 特定市场列表 */
    @Contract(pure = true)
    public static @NotNull String
    userMarketListing(String userId, String weaponName) {
        return userMarketListings(userId) + "/{weaponName}";
    }

    // 路由常量（用于路由配置）
    public static final String GET_ALL_USER_UUIDS           = USER_UUIDS;
    public static final String GET_USER_INFO                = user("{userId}");
    public static final String GET_USER_CONTACTS            = userContacts("{userId}");
    public static final String GET_USER_INVENTORY           = userInventory("{userId}");
    public static final String GET_USER_MARKET_LISTINGS     = userMarketListings("{userId}");
    public static final String GET_USER_MARKET_LISTING_IDS  = userMarketListings("{userId}") + "/ids";
    public static final String CREATE_USER                  = USERS;
    public static final String ADD_CONTACT                  = userContacts("{userId}");
    public static final String REMOVE_CONTACT               = userContact("{userId}", "{contactName}");
    public static final String ADD_WEAPON_TO_INVENTORY      = userInventoryWeapons("{userId}");
    public static final String REMOVE_WEAPON_FROM_INVENTORY = userInventoryWeapon("{userId}", "{weaponName}");
    public static final String ADD_WEAPON_TO_MARKET         = userMarketListings("{userId}");
    public static final String REMOVE_WEAPON_FROM_MARKET    = userMarketListing("{userId}", "{weaponName}");
    public static final String DELETE_USER                  = user("{userId}");
}