package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.service.StatsService;
import ru.practicum.ewm.controller.StatsController;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Test
    void hitReturns201AndDelegatesToService() throws Exception {
        String body = """
                {
                  "app": "ewm-main-service",
                  "uri": "/events/1",
                  "ip": "192.163.0.1",
                  "timestamp": "2026-10-22 12:00:00"
                }
                """;

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<EndpointHitDto> captor = ArgumentCaptor.forClass(EndpointHitDto.class);
        verify(statsService).saveHit(captor.capture());

        EndpointHitDto dto = captor.getValue();
        assertThat(dto.app()).isEqualTo("ewm-main-service");
        assertThat(dto.uri()).isEqualTo("/events/1");
        assertThat(dto.ip()).isEqualTo("192.163.0.1");
        assertThat(dto.timestamp()).isEqualTo(LocalDateTime.of(2026, 10, 22, 12, 0, 0));
    }

    @Test
    void getStatsReturns200AndDelegatesToService() throws Exception {
        when(statsService.getStats(
                any(),
                any(),
                any(),
                eq(true)
        )).thenReturn(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 6L)));

        mockMvc.perform(get("/stats")
                        .param("start", "2026-10-22 12:00:00")
                        .param("end", "2026-10-22 13:00:00")
                        .param("uris", "/events/1")
                        .param("uris", "/events/2")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {
                            "app": "ewm-main-service",
                            "uri": "/events/1",
                            "hits": 6
                          }
                        ]
                        """));
    }
}