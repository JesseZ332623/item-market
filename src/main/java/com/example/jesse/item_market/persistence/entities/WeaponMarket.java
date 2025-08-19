package com.example.jesse.item_market.persistence.entities;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/** 武器市场实体类。*/
@Data
@ToString
@Accessors(chain = true)
@Table(name = "weapons_market")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"weaponId"})
public class WeaponMarket
{
    /** 武器 ID */
    @Id
    @Column("weapon_id")
    private String weaponId;

    /** 武器名 */
    @Column("weapon_name")
    private String weaponName;

    /** 武器价格 */
    @Column("weapon_price")
    private BigDecimal weaponPrice;

    /** 武器卖家 UUID */
    @Column("weapon_sller")
    private String weaponSeller;
}
