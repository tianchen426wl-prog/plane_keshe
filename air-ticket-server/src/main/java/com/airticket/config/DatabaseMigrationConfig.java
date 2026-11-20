package com.airticket.config;

import com.airticket.database.DatabaseMigrations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.*;

@Configuration
public class DatabaseMigrationConfig {
    @Bean
    public FlywayMigrationStrategy projectMigrations(@Value("${app.database.require-empty:false}") boolean requireEmpty) {
        return flyway -> DatabaseMigrations.migrate(flyway, requireEmpty);
    }
}
