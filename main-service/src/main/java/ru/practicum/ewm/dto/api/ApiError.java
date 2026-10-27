package ru.practicum.ewm.dto.api;

import java.util.List;

public record ApiError(
        List<String> errors,
        String message,
        String reason,
        String status,
        String timestamp
) {
}