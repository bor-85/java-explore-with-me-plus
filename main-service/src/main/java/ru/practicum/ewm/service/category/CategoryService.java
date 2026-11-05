package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.category.CategoryMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.repository.category.CategoryRepository;
import ru.practicum.ewm.repository.event.EventRepository;

import java.util.List;

import static ru.practicum.ewm.exception.ErrorMessages.CATEGORY_ALREADY_EXISTS;
import static ru.practicum.ewm.exception.ErrorMessages.CATEGORY_IS_NOT_EMPTY;
import static ru.practicum.ewm.exception.ErrorMessages.CATEGORY_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        if (categoryRepository.existsByName(newCategoryDto.name())) {
            throw new ConflictException(
                    CATEGORY_ALREADY_EXISTS.formatted(newCategoryDto.name())
            );
        }

        Category category = categoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = getCategoryOrThrow(catId);

        if (categoryRepository.existsByNameAndIdNot(categoryDto.name(), catId)) {
            throw new ConflictException(
                    CATEGORY_ALREADY_EXISTS.formatted(categoryDto.name())
            );
        }

        category.setName(categoryDto.name());
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryOrThrow(catId);

        if (eventRepository.existsByCategory_Id(catId)) {
            throw new ConflictException(CATEGORY_IS_NOT_EMPTY);
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        return categoryRepository.findAllPaged(from, size).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(Long catId) {
        return categoryMapper.toDto(getCategoryOrThrow(catId));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(
                        CATEGORY_NOT_FOUND.formatted(catId)
                ));
    }
}