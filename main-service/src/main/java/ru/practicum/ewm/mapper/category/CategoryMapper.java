package ru.practicum.ewm.mapper.category;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryDto;
import ru.practicum.ewm.model.category.Category;

@Component
public class CategoryMapper {
    public CategoryDto toDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }

    public Category toEntity(NewCategoryDto dto) {
        Category category = new Category();
        category.setName(dto.name());
        return category;
    }

    public void update(Category category, String name) {
        category.setName(name);
    }
}