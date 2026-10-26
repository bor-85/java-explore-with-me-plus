package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.model.Hit;
import ru.practicum.ewm.repository.HitRepository;
import ru.practicum.ewm.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private HitRepository hitRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void saveHitSavesEntityToRepository() {
        EndpointHitDto dto = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.of(2026, 10, 22, 12, 0, 0)
        );

        statsService.saveHit(dto);

        ArgumentCaptor<Hit> captor = ArgumentCaptor.forClass(Hit.class);
        verify(hitRepository).save(captor.capture());

        Hit saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getApp()).isEqualTo("ewm-main-service");
        assertThat(saved.getUri()).isEqualTo("/events/1");
        assertThat(saved.getIp()).isEqualTo("192.163.0.1");
        assertThat(saved.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 10, 22, 12, 0, 0));
    }

    @Test
    void getStatsUsesRegularQueryWhenUniqueFalseAndUrisNull() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 22, 12, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 22, 13, 0, 0);

        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 2L)
        );

        when(hitRepository.findStats(start, end)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, null, false);

        assertThat(result).isEqualTo(expected);
        verify(hitRepository).findStats(start, end);
        verifyNoMoreInteractions(hitRepository);
    }

    @Test
    void getStatsUsesUniqueQueryWithUris() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 22, 12, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 22, 13, 0, 0);
        List<String> uris = List.of("/events/1", "/events/2");

        List<ViewStatsDto> expected = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 1L)
        );

        when(hitRepository.findUniqueStatsByUris(start, end, uris)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(start, end, uris, true);

        assertThat(result).isEqualTo(expected);
        verify(hitRepository).findUniqueStatsByUris(start, end, uris);
        verifyNoMoreInteractions(hitRepository);
    }

    @Test
    void getStatsThrowsBadRequestWhenStartAfterEnd() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 22, 13, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 22, 12, 0, 0);

        assertThatThrownBy(() -> statsService.getStats(start, end, null, false))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }
}
