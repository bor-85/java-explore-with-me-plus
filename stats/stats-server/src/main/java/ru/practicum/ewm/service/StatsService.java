package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.model.Hit;
import ru.practicum.ewm.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<Hit> hits = (uris == null || uris.isEmpty())
                ? hitRepository.findAllByTimestampBetween(start, end)
                : hitRepository.findAllByTimestampBetweenAndUriIn(start, end, uris);

        Map<StatsKey, List<Hit>> grouped = hits.stream()
                .collect(Collectors.groupingBy(hit -> new StatsKey(hit.getApp(), hit.getUri())));

        return grouped.entrySet().stream()
                .map(entry -> {
                    StatsKey key = entry.getKey();
                    List<Hit> groupHits = entry.getValue();

                    long count = unique
                            ? groupHits.stream()
                            .map(Hit::getIp)
                            .distinct()
                            .count()
                            : groupHits.size();

                    return new ViewStatsDto(key.app(), key.uri(), count);
                })
                .sorted(Comparator.comparing(ViewStatsDto::hits).reversed()
                        .thenComparing(ViewStatsDto::app)
                        .thenComparing(ViewStatsDto::uri))
                .toList();
    }

    private record StatsKey(String app, String uri) {
    }
}
