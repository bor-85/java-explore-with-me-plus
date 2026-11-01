package ru.practicum.ewm.dto.request;

import java.time.LocalDateTime;

public record ParticipationRequestDto(
        Long id,
        Long event,
        Long requester,
        LocalDateTime created,
        String status
) {
}
