package ru.practicum.ewm.comment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.comment.Comment;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.repository.comment.CommentRepository;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.ewm.service.comment.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createCommentSavesCommentForPublishedEvent() {
        User author = buildUser(2L);
        Event event = buildEvent(1L, EventState.PUBLISHED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(author));
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(commentRepository.save(any()))
                .thenAnswer(invocation -> {
                    Comment comment = invocation.getArgument(0);
                    comment.setId(10L);
                    return comment;
                });

        CommentDto result = commentService.createComment(2L, 1L, new NewCommentDto("Great event"));

        assertEquals(10L, result.id());
        assertEquals("Great event", result.text());
        assertEquals(1L, result.eventId());
        assertEquals(2L, result.author().id());
        assertNotNull(result.created());
        assertNull(result.updated());
    }

    @Test
    void createCommentThrowsNotFoundWhenUserMissing() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.createComment(2L, 1L, new NewCommentDto("Great event")));

        verify(eventRepository, never()).findById(anyLong());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentThrowsNotFoundWhenEventMissing() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(buildUser(2L)));
        when(eventRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.createComment(2L, 1L, new NewCommentDto("Great event")));

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createCommentThrowsConflictWhenEventNotPublished() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(buildUser(2L)));
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(buildEvent(1L, EventState.PENDING)));

        assertThrows(ConflictException.class,
                () -> commentService.createComment(2L, 1L, new NewCommentDto("Great event")));

        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateCommentChangesTextAndSetsUpdated() {
        Comment comment = buildComment(10L, buildUser(2L));

        when(userRepository.existsById(2L))
                .thenReturn(true);
        when(commentRepository.findByIdAndAuthor_Id(10L, 2L))
                .thenReturn(Optional.of(comment));

        CommentDto result = commentService.updateComment(2L, 10L, new NewCommentDto("Updated"));

        assertEquals("Updated", result.text());
        assertNotNull(result.updated());
    }

    @Test
    void updateCommentThrowsNotFoundWhenUserIsNotAuthor() {
        when(userRepository.existsById(3L))
                .thenReturn(true);
        when(commentRepository.findByIdAndAuthor_Id(10L, 3L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.updateComment(3L, 10L, new NewCommentDto("Updated")));
    }

    @Test
    void updateCommentThrowsNotFoundWhenUserMissing() {
        when(userRepository.existsById(3L))
                .thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> commentService.updateComment(3L, 10L, new NewCommentDto("Updated")));

        verify(commentRepository, never()).findByIdAndAuthor_Id(anyLong(), anyLong());
    }

    @Test
    void deleteUserCommentDeletesOwnComment() {
        Comment comment = buildComment(10L, buildUser(2L));

        when(userRepository.existsById(2L))
                .thenReturn(true);
        when(commentRepository.findByIdAndAuthor_Id(10L, 2L))
                .thenReturn(Optional.of(comment));

        commentService.deleteUserComment(2L, 10L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteUserCommentThrowsNotFoundWhenUserIsNotAuthor() {
        when(userRepository.existsById(3L))
                .thenReturn(true);
        when(commentRepository.findByIdAndAuthor_Id(10L, 3L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.deleteUserComment(3L, 10L));

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void getUserCommentsThrowsNotFoundWhenUserMissing() {
        when(userRepository.existsById(2L))
                .thenReturn(false);

        assertThrows(NotFoundException.class, () -> commentService.getUserComments(2L, 0, 10));

        verify(commentRepository, never()).findAuthorPage(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getEventCommentsReturnsPage() {
        Comment comment = buildComment(10L, buildUser(2L));

        when(eventRepository.existsByIdAndState(1L, EventState.PUBLISHED))
                .thenReturn(true);
        when(commentRepository.findEventPage(1L, 0, 10))
                .thenReturn(List.of(comment));

        List<CommentDto> result = commentService.getEventComments(1L, 0, 10);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    void getEventCommentsThrowsNotFoundWhenEventNotPublished() {
        when(eventRepository.existsByIdAndState(1L, EventState.PUBLISHED))
                .thenReturn(false);

        assertThrows(NotFoundException.class, () -> commentService.getEventComments(1L, 0, 10));

        verify(commentRepository, never()).findEventPage(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getCommentThrowsNotFoundWhenMissing() {
        when(commentRepository.findWithAuthorById(10L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.getComment(10L));
    }

    @Test
    void deleteCommentDeletesExistingComment() {
        when(commentRepository.existsById(10L))
                .thenReturn(true);

        commentService.deleteComment(10L);

        verify(commentRepository).deleteById(10L);
    }

    @Test
    void deleteCommentThrowsNotFoundWhenMissing() {
        when(commentRepository.existsById(10L))
                .thenReturn(false);

        assertThrows(NotFoundException.class, () -> commentService.deleteComment(10L));

        verify(commentRepository, never()).deleteById(anyLong());
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@mail.ru");
        return user;
    }

    private Event buildEvent(Long id, EventState state) {
        Event event = new Event();
        event.setId(id);
        event.setState(state);
        return event;
    }

    private Comment buildComment(Long id, User author) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setText("Great event");
        comment.setEvent(buildEvent(1L, EventState.PUBLISHED));
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        return comment;
    }
}
