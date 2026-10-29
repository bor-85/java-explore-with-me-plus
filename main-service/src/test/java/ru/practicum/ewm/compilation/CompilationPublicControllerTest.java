package ru.practicum.ewm.compilation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.compilation.CompilationPublicController;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.RestExceptionHandler;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompilationPublicController.class)
@Import(RestExceptionHandler.class)
class CompilationPublicControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    @Test
    void getCompilationsReturnsList() throws Exception {
        when(compilationService.getCompilations(isNull(), eq(0), eq(10)))
                .thenReturn(List.of(new CompilationDto(1L, true, "Pinned", List.of())));

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].pinned").value(true))
                .andExpect(jsonPath("$[0].title").value("Pinned"))
                .andExpect(jsonPath("$[0].events").isArray());
    }

    @Test
    void getCompilationsReturnsEmptyListWhenNothingFound() throws Exception {
        when(compilationService.getCompilations(eq(true), eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/compilations").param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getCompilationsReturnsBadRequestWhenFromInvalid() throws Exception {
        mockMvc.perform(get("/compilations").param("from", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(compilationService);
    }

    @Test
    void getCompilationReturnsDto() throws Exception {
        when(compilationService.getCompilation(1L))
                .thenReturn(new CompilationDto(1L, false, "Summer", List.of()));

        mockMvc.perform(get("/compilations/{compId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pinned").value(false))
                .andExpect(jsonPath("$.title").value("Summer"))
                .andExpect(jsonPath("$.events").isArray());
    }

    @Test
    void getCompilationReturnsNotFoundWhenCompilationMissing() throws Exception {
        when(compilationService.getCompilation(1L))
                .thenThrow(new NotFoundException("Compilation with id=1 was not found"));

        mockMvc.perform(get("/compilations/{compId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Compilation with id=1 was not found"));
    }

    @Test
    void getCompilationReturnsBadRequestWhenIdInvalid() throws Exception {
        mockMvc.perform(get("/compilations/ad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(compilationService);
    }
}