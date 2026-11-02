package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;

public record UpdateEventAdminRequest(
        @Size(min = 20, max = 2000)
        String annotation,

        Long category,

        @Size(min = 20, max = 7000)
        String description,

        @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN)
        LocalDateTime eventDate,

        @Valid
        Location location,

        Boolean paid,

        Integer participantLimit,

        Boolean requestModeration,

        @Pattern(regexp = "PUBLISH_EVENT|REJECT_EVENT")
        String stateAction,

        @Size(min = 3, max = 120)
        String title
) implements UpdateEventRequest {
}
