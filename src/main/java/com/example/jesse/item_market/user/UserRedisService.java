package com.example.jesse.item_market.user;

import com.example.jesse.item_market.user.dto.UserInfo;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 用户 Redis 操作接口类。*/
public interface UserRedisService
{
    /** 获取所有用户的 UUID。*/
    Flux<String> getAllUserUUID();

    /** 获取某个用户的最近联系人列表。*/
    Flux<String> getContactListByUUID(String uuid);

    /** 获取某个用户包裹中的所有武器。*/
    Flux<Weapons> getAllWeaponsFromInventoryByUUID(String uuid);

    /** 获取某个用户上架至市场的所有武器。*/
    Flux<Weapons> getAllWeaponsFromMarketByUUID(String uuid);

    /** 获取某个用户上架至市场的所有武器的 ID。*/
    Flux<String> getAllWeaponIdsFromMarketByUUID(String uuid);

    /** 获取某个用户的数据。*/
    Mono<UserInfo> getUserInfoByUUID(String uuid);

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
    Mono<String> addNewUser(String userName);

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
    Mono<Void> addNewContact(String uuid, String contactName);

    /**
     * 用户删除自己最近联系人列表的某个用户。
     *
     * @param uuid         哪个用户要删除一条最近联系人？
     * @param contactName  要删除哪个用户？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> removeContact(String uuid, String contactName);

    /**
     * 为用户的包裹添加一件武器。
     *
     * @param uuid   用户的 uuid
     * @param weapon 武器类型
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> addWeaponToInventory(String uuid, @NotNull Weapons weapon);

    /**
     * 用户销毁包裹中的某个武器。
     *
     * @param uuid   用户的 uuid
     * @param weapon 武器类型
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void> destroyWeaponFromInventory(String uuid, @NotNull Weapons weapon);

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
    Mono<String> addWeaponToMarket(String uuid, @NotNull Weapons weapon, double price);

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
    Mono<Void> removeWeaponFromMarket(String uuid, @NotNull Weapons weapon);

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
    Mono<Void> deleteUser(String uuid);
}
