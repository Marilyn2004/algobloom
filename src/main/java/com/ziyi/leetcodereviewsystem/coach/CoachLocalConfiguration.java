package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.context.annotation.*;
import com.zaxxer.hikari.*;

@Configuration
@Profile("coach")
public class CoachLocalConfiguration {
    @org.springframework.boot.flyway.autoconfigure.FlywayDataSource
    @Bean(destroyMethod="close")
    HikariDataSource dataSource() {
        // Literal local target, not Spring datasource-bound. Never points at Railway.
        HikariConfig config=new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/leetcode_coach_dev");
        config.setUsername("marilyn"); config.setPassword("");
        return new HikariDataSource(config);
    }
}
