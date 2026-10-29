package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.category.CategoryMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.repository.category.CategoryRepository;

import static ru.practicum.ewm.exception.ErrorMessages.*;


@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        categoryRepository.findByNameIgnoreCase(newCategoryDto.name())
                .ifPresent(category -> {
                    throw new ConflictException(
                            CATEGORY_ALREADY_EXISTS.formatted(newCategoryDto.name())
                    );
                });

        Category category = categoryMapper.toEntity(newCategoryDto);

        try {
            return categoryMapper.toDto(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    CATEGORY_ALREADY_EXISTS.formatted(newCategoryDto.name())
            );
        }
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = getCategoryOrThrow(catId);

        categoryRepository.findByNameIgnoreCase(categoryDto.name())
                .ifPresent(foundCategory -> {
                    if (!foundCategory.getId().equals(catId)) {
                        throw new ConflictException(
                                CATEGORY_ALREADY_EXISTS.formatted(categoryDto.name())
                        );
                    }
                });

        categoryMapper.update(category, categoryDto.name());

        try {
            return categoryMapper.toDto(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    CATEGORY_ALREADY_EXISTS.formatted(categoryDto.name())
            );
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryOrThrow(catId);

        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(CATEGORY_IS_NOT_EMPTY);
        }
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(
                        CATEGORY_NOT_FOUND.formatted(catId)
                ));
    }
}