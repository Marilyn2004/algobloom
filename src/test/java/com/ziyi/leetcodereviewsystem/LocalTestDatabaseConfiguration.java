package com.ziyi.leetcodereviewsystem;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class LocalTestDatabaseConfiguration {
    static final String URL = "jdbc:postgresql://127.0.0.1:5432/leetcode_review_test";

    @org.springframework.boot.flyway.autoconfigure.FlywayDataSource
    @Bean(destroyMethod = "close")
    HikariDataSource dataSource() {
        // Not configuration-bound: external properties cannot redirect this pool.
        // This bean disables Boot datasource auto-configuration before Hibernate starts.
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername("marilyn");
        config.setPassword("");
        config.setMaximumPoolSize(3);
        return new HikariDataSource(config);
    }
}
