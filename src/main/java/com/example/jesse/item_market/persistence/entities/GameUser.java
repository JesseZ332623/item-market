package com.example.jesse.item_market.persistence.entities;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/** 用户实体。*/
@Data
@ToString
@Accessors(chain = true)
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"uuid"})
public class GameUser
{
    /** 用户唯一 ID */
    @Id
    @Column("uuid")
    private String uuid;

    /** 用户名 */
    @Column("name")
    private String name;

    /** 用户资金 */
    @Column("funds")
    private BigDecimal funds;

    /** 用户所在公会 ID */
    @Column("guild_id")
    private Long guildId;

    /** 用户所在公会角色 */
    @Column("guild_role")
    private String guildRole;
}