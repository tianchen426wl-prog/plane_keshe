package com.airticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirTicketApplication {
    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--database-init-only=true")) {
            com.airticket.database.DatabaseSetupApplication.run(args);
            return;
        }
        SpringApplication.run(AirTicketApplication.class, args);
    }
}
