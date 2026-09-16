package in.bulletbeats.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${app.flyway.repair-on-migrate}") boolean repairOnMigrate) {
        return (Flyway flyway) -> {
            if (repairOnMigrate) {
                log.warn("app.flyway.repair-on-migrate=true — running Flyway repair before migrate");
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
