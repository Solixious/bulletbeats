package in.bulletbeats;

import in.bulletbeats.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class BulletbeatsApplication {

	public static void main(String[] args) {
		// The café operates in India — all timestamps (bill creation, price/availability
		// history, auditing) must be IST, regardless of the host/container's own timezone.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(BulletbeatsApplication.class, args);
	}

}
