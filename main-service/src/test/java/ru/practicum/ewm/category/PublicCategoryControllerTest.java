package ru.practicum.ewm.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.category.PublicCategoryController;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.RestExceptionHandler;
import ru.practicum.ewm.service.category.CategoryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCategoryController.class)
@Import(RestExceptionHandler.class)
class PublicCategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getCategoriesReturnsList() throws Exception {
        when(categoryService.getCategories(0, 10))
                .thenReturn(List.of(
                        new CategoryDto(1L, "Music"),
                        new CategoryDto(2L, "Cinema")
                ));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Music"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Cinema"));
    }

    @Test
    void getCategoriesReturnsEmptyListWhenNothingFound() throws Exception {
        when(categoryService.getCategories(eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getCategoriesReturnsBadRequestWhenFromInvalid() throws Exception {
        mockMvc.perform(get("/categories").param("from", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(categoryService);
    }

    @Test
    void getCategoriesReturnsBadRequestWhenSizeInvalid() throws Exception {
        mockMvc.perform(get("/categories").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(categoryService);
    }

    @Test
    void getCategoryReturnsDto() throws Exception {
        when(categoryService.getCategory(1L))
                .thenReturn(new CategoryDto(1L, "Music"));

        mockMvc.perform(get("/categories/{catId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Music"));
    }

    @Test
    void getCategoryReturnsNotFoundWhenCategoryMissing() throws Exception {
        when(categoryService.getCategory(1L))
                .thenThrow(new NotFoundException("Category with id=1 was not found"));

        mockMvc.perform(get("/categories/{catId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Category with id=1 was not found"));
    }

    @Test
    void getCategoryReturnsBadRequestWhenIdInvalid() throws Exception {
        mockMvc.perform(get("/categories/ad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));

        verifyNoInteractions(categoryService);
    }
}