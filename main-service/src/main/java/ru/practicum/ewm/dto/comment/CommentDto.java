package ru.practicum.ewm.dto.comment;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.time.LocalDateTime;

public record CommentDto(
        Long id,
        String text,
        Long eventId,
        UserShortDto author,
        @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN)
        LocalDateTime created,
        @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN)
        LocalDateTime updated
) {
}
