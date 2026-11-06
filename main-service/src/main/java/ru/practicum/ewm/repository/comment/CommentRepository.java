package ru.practicum.ewm.repository.comment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.comment.Comment;
import ru.practicum.ewm.model.event.EventState;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.author
            WHERE c.event.id = :eventId
            ORDER BY c.id
            LIMIT :size OFFSET :from
            """)
    List<Comment> findEventPage(@Param("eventId") long eventId, @Param("from") int from, @Param("size") int size);

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.author
            WHERE c.author.id = :authorId
            ORDER BY c.id
            LIMIT :size OFFSET :from
            """)
    List<Comment> findAuthorPage(@Param("authorId") long authorId, @Param("from") int from, @Param("size") int size);

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findByIdAndEvent_State(Long id, EventState state);

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findByIdAndAuthor_Id(Long id, Long authorId);
}
