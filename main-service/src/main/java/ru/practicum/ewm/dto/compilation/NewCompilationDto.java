package ru.practicum.ewm.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewCompilationDto(
        List<Long> events,
        Boolean pinned,
        @NotBlank
        @Size(max = 50)
        String title
) {
}
