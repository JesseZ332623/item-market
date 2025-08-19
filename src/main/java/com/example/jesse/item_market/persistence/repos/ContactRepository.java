package com.example.jesse.item_market.persistence.repos;

import com.example.jesse.item_market.persistence.entities.Contact;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 用户常用联系人实体仓储类。
 */
@Repository
public interface ContactRepository
    extends R2dbcRepository<Contact, Long>
{
    /**
     * 当某用户的最近联系人数量大于等于 limits 时，
     * 移除该用户添加时间最早的那几个联系人，维持联系人数量为 limits。
     *
     * @param uuid   哪个用户的常用联系人？
     * @param limits 联系人表最多有多少行数据？
     * @return 不发布任何数据的 Mono，表示操作整体是否完成
     */
    @Query("""
        WITH new_contact AS (
            INSERT INTO contacts(uuid, contact_name, created_at)
            VALUES (:uuid, :contactName, NOW())
            RETURNING *
        ),
        del_old AS (
             DELETE FROM contacts
             WHERE id IN (
                 SELECT id FROM contacts
                 WHERE uuid = :uuid
                 AND id NOT IN (
                     SELECT id FROM (
                         SELECT id
                         FROM contacts
                         WHERE uuid = :uuid
                         ORDER BY created_at DESC
                         LIMIT :limits
                     ) AS keep
                 )
             )
        )
        SELECT 1
        """)
    Mono<Void>
    atomicUpsertContact(
        @Param("uuid")        String uuid,
        @Param("contactName") String contactName,
        @Param("limits")      Integer limits
    );
}