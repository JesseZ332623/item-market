package com.example.jesse.item_market.persistence.entities;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 用户包裹实体。*/
@Data
@ToString
@Accessors(chain = true)
@Table(name = "inventories")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"itemId"})
public class Inventory
{
    /** 物品 ID */
    @Id
    @Column("item_id")
    private Long itemId;

    /** 物品名 */
    @Column("item_name")
    private String itemName;

    /** 该物品属于谁？ */
    @Column("uuid")
    private String uuid;
}
