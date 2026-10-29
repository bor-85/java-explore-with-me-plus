package ru.practicum.ewm.category;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.category.CategoryMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.repository.category.CategoryRepository;
import ru.practicum.ewm.service.category.CategoryServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void createCategoryCreatesCategoryWhenNameIsFree() {
        NewCategoryDto dto = new NewCategoryDto("Music");

        Category category = new Category();
        category.setName("Music");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Music");

        when(categoryRepository.findByNameIgnoreCase("Music"))
                .thenReturn(Optional.empty());
        when(categoryMapper.toEntity(dto))
                .thenReturn(category);
        when(categoryRepository.saveAndFlush(category))
                .thenReturn(savedCategory);
        when(categoryMapper.toDto(savedCategory))
                .thenReturn(new CategoryDto(1L, "Music"));

        CategoryDto result = categoryService.createCategory(dto);

        assertEquals(new CategoryDto(1L, "Music"), result);
        verify(categoryRepository).findByNameIgnoreCase("Music");
        verify(categoryRepository).saveAndFlush(category);
        verify(categoryMapper).toDto(savedCategory);
    }

    @Test
    void createCategoryThrowsConflictWhenNameExists() {
        NewCategoryDto dto = new NewCategoryDto("Music");

        Category existingCategory = new Category();
        existingCategory.setId(1L);
        existingCategory.setName("Music");

        when(categoryRepository.findByNameIgnoreCase("Music"))
                .thenReturn(Optional.of(existingCategory));

        assertThrows(ConflictException.class, () -> categoryService.createCategory(dto));

        verify(categoryRepository).findByNameIgnoreCase("Music");
        verify(categoryRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteCategoryDeletesExistingCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(category);
        verify(categoryRepository).flush();
    }

    @Test
    void deleteCategoryThrowsNotFoundWhenCategoryMissing() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(1L));

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
