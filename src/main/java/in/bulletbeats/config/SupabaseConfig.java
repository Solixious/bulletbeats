package in.bulletbeats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SupabaseConfig {

    @Bean
    public RestClient supabaseRestClient() {
        return RestClient.create();
    }
}
