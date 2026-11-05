package ru.practicum.ewm.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.client.StatClient;
import ru.practicum.ewm.controller.comment.AdminCommentController;
import ru.practicum.ewm.controller.comment.PrivateCommentController;
import ru.practicum.ewm.controller.comment.PublicCommentController;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.RestExceptionHandler;
import ru.practicum.ewm.service.comment.CommentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PrivateCommentController.class, PublicCommentController.class, AdminCommentController.class})
@Import(RestExceptionHandler.class)
class CommentControllerTest {
    private static final CommentDto COMMENT = new CommentDto(
            10L, "Great event", 1L, new UserShortDto(2L, "User"),
            LocalDateTime.of(2030, 1, 1, 12, 0), null);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @MockBean
    private StatClient statClient;

    @Test
    void createCommentReturnsCreatedComment() throws Exception {
        when(commentService.createComment(eq(2L), eq(1L), any()))
                .thenReturn(COMMENT);

        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great event\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.text").value("Great event"))
                .andExpect(jsonPath("$.eventId").value(1))
                .andExpect(jsonPath("$.author.id").value(2))
                .andExpect(jsonPath("$.created").value("2030-01-01 12:00:00"));
    }

    @Test
    void createCommentReturnsBadRequestWhenTextBlank() throws Exception {
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(commentService);
    }

    @Test
    void createCommentReturnsBadRequestWhenTextTooLong() throws Exception {
        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"%s\"}".formatted("a".repeat(2001))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void createCommentReturnsConflictWhenEventNotPublished() throws Exception {
        when(commentService.createComment(eq(2L), eq(1L), any()))
                .thenThrow(new ConflictException("Event status is not PUBLISHED"));

        mockMvc.perform(post("/users/{userId}/events/{eventId}/comments", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great event\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Event status is not PUBLISHED"));
    }

    @Test
    void updateCommentReturnsUpdatedComment() throws Exception {
        when(commentService.updateComment(eq(2L), eq(10L), any()))
                .thenReturn(COMMENT);

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", 2L, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great event\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateCommentReturnsNotFoundWhenNotAuthor() throws Exception {
        when(commentService.updateComment(eq(3L), eq(10L), any()))
                .thenThrow(new NotFoundException("Comment with id=10 was not found"));

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", 3L, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hack\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Comment with id=10 was not found"));
    }

    @Test
    void deleteUserCommentReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/users/{userId}/comments/{commentId}", 2L, 10L))
                .andExpect(status().isNoContent());

        verify(commentService).deleteUserComment(2L, 10L);
    }

    @Test
    void getUserCommentsReturnsList() throws Exception {
        when(commentService.getUserComments(2L, 0, 10))
                .thenReturn(List.of(COMMENT));

        mockMvc.perform(get("/users/{userId}/comments", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getUserCommentsReturnsBadRequestWhenFromNegative() throws Exception {
        mockMvc.perform(get("/users/{userId}/comments", 2L).param("from", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commentService);
    }

    @Test
    void getEventCommentsReturnsList() throws Exception {
        when(commentService.getEventComments(1L, 0, 10))
                .thenReturn(List.of(COMMENT));

        mockMvc.perform(get("/events/{eventId}/comments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Great event"));
    }

    @Test
    void getCommentReturnsComment() throws Exception {
        when(commentService.getComment(10L))
                .thenReturn(COMMENT);

        mockMvc.perform(get("/comments/{commentId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void adminDeleteCommentReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/admin/comments/{commentId}", 10L))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(10L);
    }

    @Test
    void adminDeleteCommentReturnsNotFoundWhenMissing() throws Exception {
        doThrow(new NotFoundException("Comment with id=10 was not found"))
                .when(commentService).deleteComment(10L);

        mockMvc.perform(delete("/admin/comments/{commentId}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }
}
