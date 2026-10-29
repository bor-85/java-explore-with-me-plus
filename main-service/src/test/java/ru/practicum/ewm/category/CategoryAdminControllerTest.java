package ru.practicum.ewm.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.category.CategoryAdminController;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.RestExceptionHandler;
import ru.practicum.ewm.service.category.CategoryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryAdminController.class)
@Import(RestExceptionHandler.class)
class CategoryAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void createCategoryReturnsCreatedCategory() throws Exception {
        when(categoryService.createCategory(any()))
                .thenReturn(new CategoryDto(1L, "Music"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Music\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Music"));

        verify(categoryService).createCategory(any());
    }

    @Test
    void updateCategoryReturnsUpdatedCategory() throws Exception {
        when(categoryService.updateCategory(eq(1L), any()))
                .thenReturn(new CategoryDto(1L, "Concerts"));

        mockMvc.perform(patch("/admin/categories/{catId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"Concerts\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Concerts"));

        verify(categoryService).updateCategory(eq(1L), any());
    }

    @Test
    void createCategoryReturnsConflictWhenNameExists() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(new ConflictException("Category with name 'Music' already exists"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Music\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason")
                        .value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message")
                        .value("Category with name 'Music' already exists"));
    }

    @Test
    void deleteCategoryReturnsNotFoundWhenCategoryMissing() throws Exception {
        org.mockito.Mockito.doThrow(new NotFoundException("Category with id=1 was not found"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/admin/categories/{catId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Category with id=1 was not found"));
    }
}