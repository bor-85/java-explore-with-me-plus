package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.model.Hit;
import ru.practicum.ewm.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final HitRepository hitRepository;

    @Transactional
    public void saveHit(EndpointHitDto dto) {
        Hit hit = new Hit(
                null,
                dto.app(),
                dto.uri(),
                dto.ip(),
                dto.timestamp()
        );
        hitRepository.save(hit);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        boolean hasUris = uris != null && !uris.isEmpty();

        if (unique) {
            return hasUris
                    ? hitRepository.findUniqueStatsByUris(start, end, uris)
                    : hitRepository.findUniqueStats(start, end);
        } else {
            return hasUris
                    ? hitRepository.findStatsByUris(start, end, uris)
                    : hitRepository.findStats(start, end);
        }
    }
}
