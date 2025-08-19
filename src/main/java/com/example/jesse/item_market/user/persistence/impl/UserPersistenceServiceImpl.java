package com.example.jesse.item_market.user.persistence.impl;

import com.example.jesse.item_market.errorhandle.PresistenceException;
import com.example.jesse.item_market.persistence.entities.Contact;
import com.example.jesse.item_market.persistence.entities.Inventory;
import com.example.jesse.item_market.persistence.repos.ContactRepository;
import com.example.jesse.item_market.persistence.repos.GameUserRepository;
import com.example.jesse.item_market.persistence.repos.InventoryRepository;
import com.example.jesse.item_market.user.persistence.UserPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** MySQL 用户操作数据持久化实现。*/
@Slf4j
@Component
public class UserPersistenceServiceImpl implements UserPersistenceService
{
    /** 用户仓储类（MySQL 数据同步）*/
    @Autowired
    private
    GameUserRepository gameUserRepository;

    /** 用户包裹仓储类。*/
    @Autowired
    private
    InventoryRepository inventoryRepository;

    @Autowired
    private ContactRepository contactRepository;

    /** R2DBC 事务操作器。*/
    @Autowired
    private
    TransactionalOperator transactionalOperator;

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
    @Override
    public Mono<Void>
    persistanceNewUser(
        String uuid, String userName,
        BigDecimal funds, List<Inventory> inventories)
    {
        /*
         * 构建先后执行操作 1, 2 的响应式流，
         * 将这个流包装成一个事务交给 MySQL 执行。
         */
        return
        this.gameUserRepository
            .persistanceNewUser(uuid, userName, funds)
            .then(this.inventoryRepository
                      .saveAll(inventories)
                      .then())
            .as(this.transactionalOperator::transactional)
            .timeout(Duration.ofSeconds(5L))
            .onErrorResume((exception) ->
                Mono.error(
                    new PresistenceException(
                        exception.getMessage(), exception
                    )
                )
            );
    }

    /**
     * 用户记录另一个用户为最近联系人时，
     * MySQL 也要同步的保存新联系人的数据。</br>
     *
     * <strong>
     * 和 Redis 的存储逻辑一样，
     * 联系人超过阈值后自动删除最老的那个联系人。
     * </strong>
     *
     * @param uuid        哪个用户要添加一条最近联系人？
     * @param contactName 哪个用户成为了它的最近联系人？
     * @param limits      每个用户最多存储多少个最近联系人？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Override
    public Mono<Void>
    persistanceNewContact(String uuid, String contactName, int limits)
    {
        return
        this.contactRepository
            .save(
                new Contact().setContactName(contactName)
                             .setUuid(uuid)
                             .setCreatedTimeStamp(Instant.now()))
            .as(this.transactionalOperator::transactional)
            .then(
                this.contactRepository
                    .findContactAmountByUUID(uuid)
                    .filter((amounts) -> amounts > limits)
                    .flatMap((amounts) ->
                        this.contactRepository
                            .trimUserContactsByLimits(uuid, limits)
                    )
                .as(this.transactionalOperator::transactional)
            )
            .timeout(Duration.ofSeconds(8L))
            .onErrorResume((exception) ->
                Mono.error(
                    new PresistenceException(
                        exception.getMessage(), exception
                    )
                )
            );
    }
}