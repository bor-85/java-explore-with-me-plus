package ru.practicum.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatClientTest {
    private static final LocalDateTime START = LocalDateTime.of(2020, 5, 5, 0, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2035, 5, 5, 0, 0, 0);

    private MockRestServiceServer server;
    private StatClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new StatClient(builder, "http://stats:9090");
    }

    @Test
    void hitPostsDtoWithFormattedTimestamp() {
        server.expect(requestTo("http://stats:9090/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\","
                        + "\"ip\":\"192.163.0.1\",\"timestamp\":\"2022-09-06 11:00:23\"}"))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.hit(new EndpointHitDto(null, "ewm-main-service", "/events/1", "192.163.0.1",
                LocalDateTime.of(2022, 9, 6, 11, 0, 23)));

        server.verify();
    }

    @Test
    void getStatsEncodesDatesAndPassesUris() {
        server.expect(requestTo("http://stats:9090/stats?start=2020-05-05%2000:00:00&end=2035-05-05%2000:00:00"
                        + "&unique=true&uris=/events/1&uris=/events/2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":6}]",
                        MediaType.APPLICATION_JSON));

        List<ViewStatsDto> stats = client.getStats(START, END, List.of("/events/1", "/events/2"), true);

        assertEquals(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 6L)), stats);
        server.verify();
    }

    @Test
    void getStatsWithoutUrisOmitsParam() {
        server.expect(requestTo("http://stats:9090/stats?start=2020-05-05%2000:00:00&end=2035-05-05%2000:00:00"
                        + "&unique=false"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getStats(START, END, null, false).isEmpty());
        server.verify();
    }

    @Test
    void getStatsReturnsEmptyListOnServerError() {
        server.expect(requestTo("http://stats:9090/stats?start=2020-05-05%2000:00:00&end=2035-05-05%2000:00:00"
                        + "&unique=false"))
                .andRespond(withServerError());

        assertTrue(client.getStats(START, END, List.of(), false).isEmpty());
        server.verify();
    }
}
