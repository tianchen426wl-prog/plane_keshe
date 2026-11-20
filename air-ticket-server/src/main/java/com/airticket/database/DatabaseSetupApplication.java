package com.airticket.database;

import com.airticket.config.DataSourceConfig;
import com.airticket.config.DatabaseMigrationConfig;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

/** Explicit CLI source, not component-scanned: no HTTP server or booking schedulers. */
@EnableAutoConfiguration
@Import({DataSourceConfig.class, DatabaseMigrationConfig.class})
public class DatabaseSetupApplication {
    public static void run(String[] args) {
        SpringApplication app = new SpringApplication(DatabaseSetupApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext ignored = app.run(args)) {
            org.slf4j.LoggerFactory.getLogger(DatabaseSetupApplication.class).info("Database setup completed; exiting without starting HTTP or flight jobs");
        }
    }
}
