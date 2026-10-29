package ru.practicum.ewm.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.event.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.initiator
            WHERE e.initiator.id = :userId
            ORDER BY e.id
            LIMIT :size OFFSET :from
            """)
    List<Event> findAllByInitiatorId(@Param("userId") Long userId,
                                     @Param("from") int from,
                                     @Param("size") int size);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);
}
