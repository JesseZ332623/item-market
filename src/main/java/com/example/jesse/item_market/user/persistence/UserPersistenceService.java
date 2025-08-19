package com.example.jesse.item_market.user.persistence;

import com.example.jesse.item_market.errorhandle.PresistenceException;
import com.example.jesse.item_market.persistence.entities.Inventory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/** MySQL 用户操作数据持久化接口。*/
public interface UserPersistenceService
{
    /**
     * 创建一个新用户时，MySQL 也要同步的保存新用户的数据。
     *
     * @param uuid        新用户唯一 ID
     * @param userName    新用户名
     * @param funds       新用户初始资金
     * @param inventories 新用户初始武器列表
     *
     * @throws PresistenceException 持久化操作中出现错误时抛出
     *
     * @return 不发布任何数据的 Mono，仅表示操作整体是否完成
     */
    Mono<Void>
    persistanceNewUser(
        String uuid, String userName,
        BigDecimal funds, List<Inventory> inventories
    );

    /**
     * 用户记录另一个用户为最近联系人时，
     * MySQL 也要同步的保存新联系人的数据。</br>
     *
     * <strong>
     *     和 Redis 的存储逻辑一样，
     *     联系人超过阈值后自动删除最老的那个联系人。
     * </strong>
     *
     * @param uuid         哪个用户要添加一条最近联系人？
     * @param contactName  哪个用户成为了它的最近联系人？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    Mono<Void>
    persistanceNewContact(String uuid, String contactName, int limit);
}
