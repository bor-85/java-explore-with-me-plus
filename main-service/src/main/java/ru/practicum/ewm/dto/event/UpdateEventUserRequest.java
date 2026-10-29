package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateEventUserRequest(
        @Size(min = 20, max = 2000)
        String annotation,

        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,

        @Valid
        Location location,

        Boolean paid,

        @PositiveOrZero
        Integer participantLimit,

        Boolean requestModeration,

        @Pattern(regexp = "SEND_TO_REVIEW|CANCEL_REVIEW")
        String stateAction,

        @Size(min = 3, max = 120)
        String title
) {
}