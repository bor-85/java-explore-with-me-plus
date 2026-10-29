package ru.practicum.ewm.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.initiator
            WHERE (:users IS NULL OR e.initiator.id IN :users)
            AND (:states IS NULL OR e.state IN :states)
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            ORDER BY e.id
            LIMIT :size OFFSET :from
            """)
    List<Event> findAllByAdminFilters(@Param("users") List<Long> users,
                                      @Param("states") List<EventState> states,
                                      @Param("categories") List<Long> categories,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      @Param("from") int from,
                                      @Param("size") int size);

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.initiator
            WHERE e.state = ru.practicum.ewm.model.event.EventState.PUBLISHED
            AND (:text IS NULL OR LOWER(e.annotation) LIKE :text OR LOWER(e.description) LIKE :text)
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:paid IS NULL OR e.paid = :paid)
            AND e.eventDate >= :rangeStart
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            ORDER BY e.eventDate
            """)
    List<Event> findAllPublished(@Param("text") String text,
                                 @Param("categories") List<Long> categories,
                                 @Param("paid") Boolean paid,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd);

    Optional<Event> findByIdAndState(Long id, EventState state);

    @Query(value = """
            SELECT event_id, COUNT(*) FROM requests
            WHERE status = 'CONFIRMED' AND event_id IN :eventIds
            GROUP BY event_id
            """, nativeQuery = true)
    List<Object[]> countConfirmedRequests(@Param("eventIds") Collection<Long> eventIds);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);
}
