package in.bulletbeats;

import in.bulletbeats.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class BulletbeatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BulletbeatsApplication.class, args);
	}

}
