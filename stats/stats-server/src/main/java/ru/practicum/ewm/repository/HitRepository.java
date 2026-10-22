package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.Hit;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {

    List<Hit> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<Hit> findAllByTimestampBetweenAndUriIn(LocalDateTime start,
                                                LocalDateTime end,
                                                Collection<String> uris);
}
