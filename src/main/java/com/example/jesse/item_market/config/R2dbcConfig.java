package com.example.jesse.item_market.config;


import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.asyncer.r2dbc.mysql.constant.SslMode;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.Duration;

/** Spring Data R2DBC 配置类。*/
@Configuration
@EnableTransactionManagement
public class R2dbcConfig
{
    /** R2DBC 数据库池、连接工厂配置。*/
    @Bean
    public ConnectionFactory connectionFactory()
    {
        return
        new ConnectionPool(
            ConnectionPoolConfiguration
                .builder()
                .connectionFactory(
                    MySqlConnectionFactory
                        .from(
                            MySqlConnectionConfiguration
                                .builder()
                                .host("localhost")
                                .port(3306)
                                .username("Jesse_EC233")
                                .password("3191955858_EC")
                                .database("item_market")
                                /* 启用 TCP keep-alive 机制 */
                                .tcpKeepAlive(true)
                                /* 设置连接超时 */
                                .connectTimeout(Duration.ofSeconds(30L))
                                /* 禁用 SSL */
                                .sslMode(SslMode.DISABLED)
                                /* 不自动检测扩展 */
                                .autodetectExtensions(false)
                                /* 禁用 Nagle 算法 */
                                .tcpNoDelay(true)
                                .build()
                        ))
                .initialSize(20)
                .maxSize(45)
                .maxIdleTime(Duration.ofSeconds(30L))
                .build()
        );
    }

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
