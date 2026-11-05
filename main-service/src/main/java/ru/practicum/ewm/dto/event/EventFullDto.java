package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.time.LocalDateTime;

public record EventFullDto(
        Long id,
        String annotation,
        CategoryDto category,
        Long confirmedRequests,
        @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN)
        LocalDateTime createdOn,
        String description,
        @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN)
        LocalDateTime eventDate,
        UserShortDto initiator,
        Location location,
        Boolean paid,
        Integer participantLimit,
        @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN)
        LocalDateTime publishedOn,
        Boolean requestModeration,
        String state,
        String title,
        Long views
) {
}
