package com.example.jesse.item_market.persistence.repos;

import com.example.jesse.item_market.persistence.entities.GameUser;
import com.example.jesse.item_market.utils.UUIDGenerator;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/** 用户数据实体仓储类。*/
@Repository
public interface GameUserRepository
    extends R2dbcRepository<GameUser, String>
{
    /**
     * 持久化存储一个新用户。
     * 由于新用户的 UUID 不是 AUTO_INCREMENT 维护的，
     * 而是使用 {@link UUIDGenerator} 生成的，
     * 而对于实体类中被 @Id 注解的字段不为默认值或者 null 的情况下，
     * R2DBC 的 save() 操作可能会误判该数据行存在然后错误的执行 UPDATE 语句，
     * 所以这里还是要使用 @Query 注解，显式的调用 INSERT 语句。
     *
     * @param uuid        新用户唯一 ID
     * @param userName    新用户名
     * @param funds       新用户初始资金
     *
     * @return 不发布任何数据的 Mono，仅表示操作整体是否完成
     */
    @Query(value = """
        INSERT INTO
            item_market.users(uuid, name, funds, guild_id, guild_role)
        VALUES
            (:uuid, :userName, :funds, NULL, NULL)
        """)
    Mono<Void>
    persistanceNewUser(
        @Param("uuid")     String uuid,
        @Param("userName") String userName,
        @Param("funds")    BigDecimal funds
    );
}
