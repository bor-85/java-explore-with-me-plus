package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.practicum.client.StatClient;

@Configuration
public class StatClientConfig {

    @Bean
    public StatClient statClient(RestClient.Builder builder,
                                 @Value("${stats.server.url:http://localhost:9090}") String serverUrl) {
        return new StatClient(builder, serverUrl);
    }
}
