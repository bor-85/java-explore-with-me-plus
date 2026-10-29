package ru.practicum.ewm.dto.compilation;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCompilationRequest(
        Boolean pinned,
        @Size(min = 1, max = 50)
        @Pattern(regexp = ".*\\S.*")
        String title,
        List<Long> events
) {
}