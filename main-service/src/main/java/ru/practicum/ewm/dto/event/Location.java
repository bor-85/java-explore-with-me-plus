package ru.practicum.ewm.dto.event;

import jakarta.validation.constraints.NotNull;

public record Location(
        @NotNull Double lat,
        @NotNull Double lon
) {
}