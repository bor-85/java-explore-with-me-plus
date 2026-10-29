package ru.practicum.ewm.repository.compilation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.compilation.Compilation;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    Optional<Compilation> findWithEventsById(Long id);

    @Query(value = """
            SELECT *
            FROM compilations c
            WHERE (:pinned IS NULL OR c.pinned = :pinned)
            ORDER BY c.id
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<Compilation> findAllPaged(@Param("pinned") Boolean pinned,
                                   @Param("from") int from,
                                   @Param("size") int size);
}