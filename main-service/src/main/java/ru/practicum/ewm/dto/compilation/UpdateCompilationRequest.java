package ru.practicum.ewm.dto.compilation;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCompilationRequest(
        List<Long> events,
        Boolean pinned,
        @Size(max = 50)
        String title
) {
}