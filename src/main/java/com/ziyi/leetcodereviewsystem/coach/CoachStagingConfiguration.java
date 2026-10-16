package com.ziyi.leetcodereviewsystem.coach;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Dedicated Render/Neon demo pool; validation runs before Flyway/Hibernate initialization. */
@Configuration
@Profile("staging")
public class CoachStagingConfiguration {
    @Bean(destroyMethod="close")
    @org.springframework.boot.flyway.autoconfigure.FlywayDataSource
    HikariDataSource dataSource(Environment environment) {
        validateTarget(environment.getRequiredProperty("RENDER"),
                environment.getRequiredProperty("RENDER_SERVICE_ID"),
                environment.getRequiredProperty("ALGOBLOOM_STAGING_SERVICE_ID"),
                environment.getRequiredProperty("ALGOBLOOM_STAGING_DB_HOST"),
                environment.getRequiredProperty("ALGOBLOOM_STAGING_JDBC_URL"));
        if (environment.matchesProfiles("coach")) throw new IllegalStateException("Local and staging profiles cannot be combined");
        var config=new HikariConfig();
        config.setJdbcUrl(environment.getRequiredProperty("ALGOBLOOM_STAGING_JDBC_URL"));
        config.setUsername(environment.getRequiredProperty("ALGOBLOOM_STAGING_DB_USER"));
        config.setPassword(environment.getRequiredProperty("ALGOBLOOM_STAGING_DB_PASSWORD"));
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        return new HikariDataSource(config);
    }
    static void validateTarget(String render,String actualId,String expectedId,String host,String jdbc) {
        if (!"true".equals(render) || !actualId.startsWith("srv-") || !actualId.equals(expectedId)
                || !host.matches("ep-[a-z0-9-]+[.][a-z0-9-]+[.](aws|azure)[.]neon[.]tech")
                || !jdbc.equals("jdbc:postgresql://"+host+":5432/neondb?sslmode=verify-full")) {
            throw new IllegalStateException("Refusing a datasource outside the pinned Render/Neon staging target");
        }
    }
}
