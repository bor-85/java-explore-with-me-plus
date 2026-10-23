package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.model.Hit;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(h))
            FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h) DESC, h.app ASC, h.uri ASC
            """)
    List<ViewStatsDto> findStats(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(h))
            FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
              AND h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h) DESC, h.app ASC, h.uri ASC
            """)
    List<ViewStatsDto> findStatsByUris(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("uris") Collection<String> uris);

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
            FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC, h.app ASC, h.uri ASC
            """)
    List<ViewStatsDto> findUniqueStats(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
            FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
              AND h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC, h.app ASC, h.uri ASC
            """)
    List<ViewStatsDto> findUniqueStatsByUris(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("uris") Collection<String> uris);
}
