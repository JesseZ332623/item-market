package com.example.jesse.item_market.persistence.entities;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 公会实体。*/
@Data
@ToString
@Accessors(chain = true)
@Table(name = "guilds")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"guildId"})
public class Guild
{
    /** 公会 ID */
    @Id
    @Column("guild_id")
    public Long guildId;

    /** 公会名 */
    @Column("guild_name")
    private String guildName;
}
