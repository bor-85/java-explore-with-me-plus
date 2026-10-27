package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record NewEventDto(
        @Size(min = 20, max = 2000)
        String annotation,

        @NotNull
        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        @Valid
        @NotNull
        Location location,

        Boolean paid,

        Integer participantLimit,

        Boolean requestModeration,

        @Size(min = 3, max = 120)
        String title
) {
}