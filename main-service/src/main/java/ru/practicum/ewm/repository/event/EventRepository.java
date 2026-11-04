package ru.practicum.ewm.repository.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Страница опубликованных событий, отсортированная по дате.
     * Фильтры, доступность и пагинация выполняются в БД.
     */
    @Query(value = """
            SELECT e.id
            FROM events e
            WHERE e.state = 'PUBLISHED'
              AND (
                    :filterText = FALSE
                    OR LOWER(e.annotation) LIKE :text
                    OR LOWER(e.description) LIKE :text
              )
              AND (
                    :filterCategories = FALSE
                    OR e.category_id IN (:categories)
              )
              AND (
                    :filterPaid = FALSE
                    OR e.paid = :paid
              )
              AND e.event_date >= :rangeStart
              AND (
                    :filterRangeEnd = FALSE
                    OR e.event_date <= :rangeEnd
              )
              AND (
                    :onlyAvailable = FALSE
                    OR e.participant_limit = 0
                    OR e.participant_limit > (
                        SELECT COUNT(*)
                        FROM requests r
                        WHERE r.event_id = e.id
                          AND r.status = 'CONFIRMED'
                    )
              )
            ORDER BY e.event_date ASC, e.id ASC
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Long> findPublishedPageIds(@Param("text") String text,
                                    @Param("filterText") boolean filterText,
                                    @Param("categories") List<Long> categories,
                                    @Param("filterCategories") boolean filterCategories,
                                    @Param("paid") boolean paid,
                                    @Param("filterPaid") boolean filterPaid,
                                    @Param("rangeStart") LocalDateTime rangeStart,
                                    @Param("rangeEnd") LocalDateTime rangeEnd,
                                    @Param("filterRangeEnd") boolean filterRangeEnd,
                                    @Param("onlyAvailable") boolean onlyAvailable,
                                    @Param("from") int from,
                                    @Param("size") int size);

    @Query(value = """
            SELECT e.id
            FROM events e
            WHERE e.state = 'PUBLISHED'
              AND (
                    :filterText = FALSE
                    OR LOWER(e.annotation) LIKE :text
                    OR LOWER(e.description) LIKE :text
              )
              AND (
                    :filterCategories = FALSE
                    OR e.category_id IN (:categories)
              )
              AND (
                    :filterPaid = FALSE
                    OR e.paid = :paid
              )
              AND e.event_date >= :rangeStart
              AND (
                    :filterRangeEnd = FALSE
                    OR e.event_date <= :rangeEnd
              )
              AND (
                    :onlyAvailable = FALSE
                    OR e.participant_limit = 0
                    OR e.participant_limit > (
                        SELECT COUNT(*)
                        FROM requests r
                        WHERE r.event_id = e.id
                          AND r.status = 'CONFIRMED'
                    )
              )
            ORDER BY e.id ASC
            """, nativeQuery = true)
    List<Long> findPublishedIdsForViewSort(@Param("text") String text,
                                           @Param("filterText") boolean filterText,
                                           @Param("categories") List<Long> categories,
                                           @Param("filterCategories") boolean filterCategories,
                                           @Param("paid") boolean paid,
                                           @Param("filterPaid") boolean filterPaid,
                                           @Param("rangeStart") LocalDateTime rangeStart,
                                           @Param("rangeEnd") LocalDateTime rangeEnd,
                                           @Param("filterRangeEnd") boolean filterRangeEnd,
                                           @Param("onlyAvailable") boolean onlyAvailable);

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.initiator
            WHERE e.id IN :eventIds
            """)
    List<Event> findAllWithRelationsByIdIn(@Param("eventIds") Collection<Long> eventIds);

    Optional<Event> findByIdAndState(Long id, EventState state);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @Query(value = """
            SELECT event_id, COUNT(*)
            FROM requests
            WHERE status = 'CONFIRMED'
              AND event_id IN :eventIds
            GROUP BY event_id
            """, nativeQuery = true)
    List<Object[]> countConfirmedRequests(@Param("eventIds") Collection<Long> eventIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findByIdForUpdate(@Param("eventId") long eventId);

    boolean existsByCategory_Id(Long categoryId);
}