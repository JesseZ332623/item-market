package com.example.jesse.item_market.persistence.repos;

import com.example.jesse.item_market.persistence.entities.Contact;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** 用户常用联系人实体仓储类。*/
@Repository
public interface ContactRepository
    extends R2dbcRepository<Contact, Long>
{
    /** 查询某个用户所拥有的联系人数量。*/
    @Query("SELECT COUNT(*) FROM contacts WHERE uuid = :uuid")
    Mono<Integer>
    findContactAmountByUUID(@Param("uuid") String uuid);

    /**
     * 当某用户的最近联系人数量大于等于 limits 时，
     * 移除该用户添加时间最早的那几个联系人，维持联系人数量为 limits。
     *
     * @param uuid   哪个用户的常用联系人？
     * @param limits 联系人表最多有多少行数据？
     *
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Query("""
       DELETE c FROM contacts c
       LEFT JOIN (
           SELECT uuid
           FROM contacts
           WHERE uuid = :uuid
           ORDER BY created_at DESC
           LIMIT :limits
       ) AS keep ON c.uuid = keep.uuid
       WHERE c.uuid = :uuid AND keep.uuid IS NULL
       """)
    Mono<Void>
    trimUserContactsByLimits(
        @Param("uuid")   String  uuid,
        @Param("limits") Integer limits
    );
}