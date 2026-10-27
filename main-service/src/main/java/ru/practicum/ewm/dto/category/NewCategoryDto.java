package ru.practicum.ewm.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewCategoryDto(
        @NotBlank
        @Size(max = 50)
        String name
) {
}