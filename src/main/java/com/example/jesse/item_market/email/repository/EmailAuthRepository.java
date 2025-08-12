package com.example.jesse.item_market.email.repository;

import com.example.jesse.item_market.email.entity.EmailAuthTable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** 邮箱数据表仓储类。*/
public interface EmailAuthRepository
    extends ReactiveCrudRepository<EmailAuthTable, Integer>
{
    @Query(value = """
            SELECT email_auth_code
            FROM email_auth_table
            WHERE email = :email
        """)
    Mono<String>
    findAuthCodeByEmail(
        @Param(value = "email") String email
    );
}
