package com.example.jesse.item_market.persistence.entities;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/** 用户常用联系人实体。*/
@Data
@ToString
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"contactId"})
@Table(name = "contacts")
public class Contact
{
    /** 联系人用户 ID */
    @Id
    @Column("contact_id")
    private Long contactId;

    /** 联系人用户名 */
    @Column("contact_name")
    private String contactName;

    /** 谁的联系人？ */
    @Column("uuid")
    private String uuid;

    /** 创建联系人时的时间戳（使用 MySQL 默认值 CURRENT_TIMESTAMP）*/
    @Column("created_at")
    private Instant createdTimeStamp;
}
