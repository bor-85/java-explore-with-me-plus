package ru.practicum.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.client.exception.StatsClientException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatClient {
    private static final Logger log = LoggerFactory.getLogger(StatClient.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(EndpointHitDto.DATE_TIME_PATTERN);

    private final RestClient restClient;

    public StatClient(RestClient.Builder builder,
                      @Value("${stats.server.url:http://localhost:9090}") String serverUrl) {
        this.restClient = builder.baseUrl(serverUrl).build();
    }

    public void hit(EndpointHitDto hit) {
        try {
            restClient.post()
                    .uri("/hit")
                    .body(hit)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Failed to send hit {}: {}", hit, e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        try {
            List<ViewStatsDto> stats = restClient.get()
                    .uri(builder -> {
                        builder.path("/stats")
                                .queryParam("start", start.format(FORMATTER))
                                .queryParam("end", end.format(FORMATTER))
                                .queryParam("unique", unique);
                        if (uris != null && !uris.isEmpty()) {
                            builder.queryParam("uris", uris);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return stats;
        } catch (RestClientException e) {
            throw new StatsClientException("Failed to get stats from stats-server", e);
        }
    }
}