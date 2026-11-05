package ru.practicum.ewm.repository.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * Страница id событий пользователя. Сами события грузятся отдельно,
     * через findAllWithRelationsByIdIn.
     */
    @Query(value = """
            SELECT e.id
            FROM events e
            WHERE e.initiator_id = :userId
            ORDER BY e.id ASC
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Long> findUserEventPageIds(@Param("userId") long userId,
                                    @Param("from") int from,
                                    @Param("size") int size);

    /**
     * Страница id событий для админки. Флаг FALSE означает, что фильтр не применяется.
     */
    @Query(value = """
            SELECT e.id
            FROM events e
            WHERE (:filterUsers = FALSE OR e.initiator_id IN (:users))
              AND (:filterStates = FALSE OR e.state IN (:states))
              AND (:filterCategories = FALSE OR e.category_id IN (:categories))
              AND (:filterRangeStart = FALSE OR e.event_date >= :rangeStart)
              AND (:filterRangeEnd = FALSE OR e.event_date <= :rangeEnd)
            ORDER BY e.id ASC
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Long> findAdminEventPageIds(@Param("filterUsers") boolean filterUsers,
                                     @Param("users") List<Long> users,
                                     @Param("filterStates") boolean filterStates,
                                     @Param("states") List<String> states,
                                     @Param("filterCategories") boolean filterCategories,
                                     @Param("categories") List<Long> categories,
                                     @Param("filterRangeStart") boolean filterRangeStart,
                                     @Param("rangeStart") LocalDateTime rangeStart,
                                     @Param("filterRangeEnd") boolean filterRangeEnd,
                                     @Param("rangeEnd") LocalDateTime rangeEnd,
                                     @Param("from") int from,
                                     @Param("size") int size);

    /**
     * Страница id опубликованных событий, отсортированная по дате.
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

    /**
     * Все id опубликованных событий, подходящих под фильтры, для сортировки по просмотрам.
     * Пагинация делается в сервисе после подсчёта просмотров.
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

    /**
     * Единственное место, где грузятся события для списков.
     * category и initiator приходят в одном запросе, порядок задаёт сервис.
     */
    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.initiator
            WHERE e.id IN :eventIds
            """)
    List<Event> findAllWithRelationsByIdIn(@Param("eventIds") Collection<Long> eventIds);

    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findByIdAndState(Long id, EventState state);

    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findWithRelationsById(Long id);

    @Query(value = """
            SELECT event_id, COUNT(*)
            FROM requests
            WHERE status = 'CONFIRMED'
              AND event_id IN :eventIds
            GROUP BY event_id
            """, nativeQuery = true)
    List<Object[]> countConfirmedRequests(@Param("eventIds") Collection<Long> eventIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findByIdForUpdate(@Param("eventId") long eventId);

    boolean existsByCategory_Id(Long categoryId);

    boolean existsByIdAndState(Long id, EventState state);
}