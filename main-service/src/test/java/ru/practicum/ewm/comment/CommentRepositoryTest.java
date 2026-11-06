package ru.practicum.ewm.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.comment.Comment;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.repository.comment.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CommentRepositoryTest {
    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    private User alice;
    private User bob;
    private Event published;
    private Event pending;
    private List<Long> publishedCommentIds;

    @BeforeEach
    void setUp() {
        alice = persistUser("alice");
        bob = persistUser("bob");
        Category category = new Category();
        category.setName("concerts");
        em.persist(category);
        published = persistEvent(alice, category, EventState.PUBLISHED);
        pending = persistEvent(alice, category, EventState.PENDING);

        publishedCommentIds = List.of(
                persistComment(published, alice).getId(),
                persistComment(published, bob).getId(),
                persistComment(published, alice).getId(),
                persistComment(published, bob).getId(),
                persistComment(published, alice).getId());
        persistComment(pending, alice);
        em.flush();
        em.clear();
    }

    @Test
    void findEventPageReturnsOrderedPageWithOffset() {
        List<Comment> page = commentRepository.findEventPage(published.getId(), 1, 3);

        assertEquals(publishedCommentIds.subList(1, 4), page.stream().map(Comment::getId).toList());
        assertEquals("bob", page.get(0).getAuthor().getName());
    }

    @Test
    void findEventPageReturnsTailWithoutDuplicates() {
        List<Comment> page = commentRepository.findEventPage(published.getId(), 3, 10);

        assertEquals(publishedCommentIds.subList(3, 5), page.stream().map(Comment::getId).toList());
    }

    @Test
    void findAuthorPageReturnsOnlyAuthorCommentsWithOffset() {
        List<Comment> page = commentRepository.findAuthorPage(alice.getId(), 1, 2);

        assertEquals(List.of(publishedCommentIds.get(2), publishedCommentIds.get(4)),
                page.stream().map(Comment::getId).toList());
        assertTrue(page.stream().allMatch(c -> c.getAuthor().getId().equals(alice.getId())));
    }

    @Test
    void findByIdAndEventStateSkipsCommentOfUnpublishedEvent() {
        Long pendingCommentId = commentRepository.findAuthorPage(alice.getId(), 3, 1).get(0).getId();

        assertTrue(commentRepository.findByIdAndEvent_State(pendingCommentId, EventState.PUBLISHED).isEmpty());
        assertTrue(commentRepository.findByIdAndEvent_State(publishedCommentIds.get(0), EventState.PUBLISHED)
                .isPresent());
    }

    private User persistUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(name + "@mail.ru");
        return em.persist(user);
    }

    private Event persistEvent(User initiator, Category category, EventState state) {
        Event event = new Event();
        event.setAnnotation("annotation");
        event.setDescription("description");
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setLat(0.0);
        event.setLon(0.0);
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setState(state);
        event.setTitle("title");
        event.setCreatedOn(LocalDateTime.now());
        return em.persist(event);
    }

    private Comment persistComment(Event event, User author) {
        Comment comment = new Comment();
        comment.setText("text");
        comment.setEvent(event);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        return em.persist(comment);
    }
}
