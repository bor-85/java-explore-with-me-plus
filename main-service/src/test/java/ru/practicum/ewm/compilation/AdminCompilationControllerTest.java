package ru.practicum.ewm.compilation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.compilation.AdminCompilationController;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.RestExceptionHandler;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCompilationController.class)
@Import(RestExceptionHandler.class)
class AdminCompilationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void createCompilationReturnsCreatedCompilation() throws Exception {
        when(compilationService.createCompilation(any()))
                .thenReturn(new CompilationDto(1L, false, "Summer", List.of()));

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[1,2],\"pinned\":false,\"title\":\"Summer\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pinned").value(false))
                .andExpect(jsonPath("$.title").value("Summer"))
                .andExpect(jsonPath("$.events").isArray());

        verify(compilationService).createCompilation(any());
    }

    @Test
    void createCompilationReturnsBadRequestWhenTitleBlank() throws Exception {
        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[1,2],\"pinned\":false,\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(compilationService);
    }

    @Test
    void createCompilationReturnsConflictWhenTitleExists() throws Exception {
        when(compilationService.createCompilation(any()))
                .thenThrow(new ConflictException("Compilation with title 'Summer' already exists"));

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[1,2],\"pinned\":false,\"title\":\"Summer\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason")
                        .value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message")
                        .value("Compilation with title 'Summer' already exists"));
    }

    @Test
    void createCompilationReturnsNotFoundWhenEventMissing() throws Exception {
        when(compilationService.createCompilation(any()))
                .thenThrow(new NotFoundException("Event with id=99 was not found"));

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[99],\"pinned\":false,\"title\":\"Summer\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Event with id=99 was not found"));
    }

    @Test
    void updateCompilationReturnsUpdatedCompilation() throws Exception {
        when(compilationService.updateCompilation(eq(1L), any()))
                .thenReturn(new CompilationDto(1L, true, "Updated", List.of()));

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[1],\"pinned\":true,\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.events").isArray());

        verify(compilationService).updateCompilation(eq(1L), any());
    }

    @Test
    void updateCompilationReturnsBadRequestWhenTitleBlank() throws Exception {
        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(compilationService);
    }

    @Test
    void updateCompilationReturnsBadRequestWhenTitleTooLong() throws Exception {
        String longTitle = "a".repeat(51);

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(longTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(compilationService);
    }

    @Test
    void updateCompilationReturnsNotFoundWhenCompilationMissing() throws Exception {
        doThrow(new NotFoundException("Compilation with id=1 was not found"))
                .when(compilationService).updateCompilation(eq(1L), any());

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[1],\"pinned\":true,\"title\":\"Updated\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Compilation with id=1 was not found"));
    }

    @Test
    void updateCompilationReturnsNotFoundWhenEventMissing() throws Exception {
        doThrow(new NotFoundException("Event with id=99 was not found"))
                .when(compilationService).updateCompilation(eq(1L), any());

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[99],\"pinned\":true,\"title\":\"Updated\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Event with id=99 was not found"));
    }

    @Test
    void deleteCompilationReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/admin/compilations/{compId}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(compilationService).deleteCompilation(1L);
    }

    @Test
    void deleteCompilationReturnsNotFoundWhenCompilationMissing() throws Exception {
        doThrow(new NotFoundException("Compilation with id=1 was not found"))
                .when(compilationService).deleteCompilation(1L);

        mockMvc.perform(delete("/admin/compilations/{compId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Compilation with id=1 was not found"));
    }
}