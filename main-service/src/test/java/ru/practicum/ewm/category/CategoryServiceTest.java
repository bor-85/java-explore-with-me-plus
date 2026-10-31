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
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.service.category.CategoryService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategoryCreatesCategoryWhenNameIsFree() {
        NewCategoryDto dto = new NewCategoryDto("Music");

        Category category = new Category();
        category.setName("Music");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Music");

        when(categoryRepository.existsByName("Music"))
                .thenReturn(false);
        when(categoryMapper.toEntity(dto))
                .thenReturn(category);
        when(categoryRepository.save(category))
                .thenReturn(savedCategory);
        when(categoryMapper.toDto(savedCategory))
                .thenReturn(new CategoryDto(1L, "Music"));

        CategoryDto result = categoryService.createCategory(dto);

        assertEquals(new CategoryDto(1L, "Music"), result);
        verify(categoryRepository).existsByName("Music");
        verify(categoryMapper).toEntity(dto);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toDto(savedCategory);
    }

    @Test
    void createCategoryThrowsConflictWhenNameExists() {
        NewCategoryDto dto = new NewCategoryDto("Music");

        when(categoryRepository.existsByName("Music"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCategory(dto));

        verify(categoryRepository).existsByName("Music");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategoryUpdatesCategoryWhenNameIsFree() {
        CategoryDto dto = new CategoryDto(1L, "Rock");

        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Rock");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Rock", 1L))
                .thenReturn(false);
        when(categoryRepository.save(category))
                .thenReturn(savedCategory);
        when(categoryMapper.toDto(savedCategory))
                .thenReturn(new CategoryDto(1L, "Rock"));

        CategoryDto result = categoryService.updateCategory(1L, dto);

        assertEquals(new CategoryDto(1L, "Rock"), result);
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).existsByNameAndIdNot("Rock", 1L);
        verify(categoryRepository).save(category);
        verify(categoryMapper).toDto(savedCategory);
    }

    @Test
    void updateCategoryThrowsConflictWhenNameExistsInAnotherCategory() {
        CategoryDto dto = new CategoryDto(1L, "Rock");

        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Rock", 1L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.updateCategory(1L, dto));

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).existsByNameAndIdNot("Rock", 1L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategoryDeletesCategoryWhenNoEventsUseIt() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(eventRepository.existsByCategory_Id(1L))
                .thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).findById(1L);
        verify(eventRepository).existsByCategory_Id(1L);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategoryThrowsConflictWhenCategoryIsNotEmpty() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(eventRepository.existsByCategory_Id(1L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.deleteCategory(1L));

        verify(categoryRepository).findById(1L);
        verify(eventRepository).existsByCategory_Id(1L);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategoryThrowsNotFoundWhenCategoryMissing() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(1L));

        verify(categoryRepository).findById(1L);
        verify(eventRepository, never()).existsByCategory_Id(any());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void updateCategoryThrowsConflictWhenNameBelongsToAnotherCategory() {
        CategoryDto dto = new CategoryDto(1L, "Rock");

        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Rock", 1L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.updateCategory(1L, dto));

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).existsByNameAndIdNot("Rock", 1L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategoryThrowsConflictWhenCategoryHasEvents() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(eventRepository.existsByCategory_Id(1L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.deleteCategory(1L));

        verify(categoryRepository).findById(1L);
        verify(eventRepository).existsByCategory_Id(1L);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void getCategoriesReturnsCategoriesList() {
        Category first = new Category();
        first.setId(1L);
        first.setName("Music");

        Category second = new Category();
        second.setId(2L);
        second.setName("Cinema");

        when(categoryRepository.findAllPaged(0, 10))
                .thenReturn(List.of(first, second));
        when(categoryMapper.toDto(first))
                .thenReturn(new CategoryDto(1L, "Music"));
        when(categoryMapper.toDto(second))
                .thenReturn(new CategoryDto(2L, "Cinema"));

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertEquals(List.of(
                new CategoryDto(1L, "Music"),
                new CategoryDto(2L, "Cinema")
        ), result);

        verify(categoryRepository).findAllPaged(0, 10);
        verify(categoryMapper).toDto(first);
        verify(categoryMapper).toDto(second);
    }

    @Test
    void getCategoryThrowsNotFoundWhenCategoryMissing() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getCategory(1L));

        verify(categoryRepository).findById(1L);
        verify(categoryMapper, never()).toDto(any());
    }
}
