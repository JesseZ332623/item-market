package com.example.jesse.item_market.config;


import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/** Spring Data R2DBC 配置类。*/
@Configuration
public class R2dbcConfig
{
    /** R2DBC 的响应式事务管理器。*/
    @Bean
    public ReactiveTransactionManager
    transactionManager(@NotNull R2dbcEntityTemplate template)
    {
        return new R2dbcTransactionManager(
            template.getDatabaseClient()
                    .getConnectionFactory()
        );
    }

    /** R2DBC 的事务操作器。*/
    @Bean
    public TransactionalOperator
    transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    /**
     * 向 R2DBC 提交几个自定义的转换器，
     * 并且阐明使用 MySQL 方言。
     */
    @Bean
    public R2dbcCustomConversions
    customConversions(DatabaseClient client)
    {
        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE);
    }
}
