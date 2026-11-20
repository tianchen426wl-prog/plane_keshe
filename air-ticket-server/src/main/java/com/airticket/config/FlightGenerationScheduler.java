package com.airticket.config;
import com.airticket.service.FlightCatalogService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class FlightGenerationScheduler {
 @Autowired private FlightCatalogService catalog;
 @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
 @Value("${flights.generation.enabled:true}") private boolean enabled;
 @EventListener(ApplicationReadyEvent.class)
 @Scheduled(cron="${flights.generation.cron:0 5 0 * * *}",zone="Asia/Shanghai")
 public void refresh(){if(enabled)try{catalog.generate(catalog.today(),catalog.windowDays());}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Flight refresh failed: {}",e.getMessage());}}
 @Scheduled(cron="0 30 3 * * *",zone="Asia/Shanghai")
 public void retention(){db.update("DELETE FROM ai_plan WHERE created_at<DATE_SUB(NOW(),INTERVAL 7 DAY)");db.update("DELETE FROM ai_call_audit WHERE created_at<DATE_SUB(NOW(),INTERVAL 30 DAY)");db.update("DELETE FROM flight_quote WHERE used_hold_id IS NULL AND expires_at<DATE_SUB(NOW(),INTERVAL 7 DAY)");}
}
