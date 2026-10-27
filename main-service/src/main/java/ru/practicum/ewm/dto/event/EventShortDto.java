package ru.practicum.ewm.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.user.UserShortDto;

import java.time.LocalDateTime;

public record EventShortDto(
        Long id,
        String annotation,
        CategoryDto category,
        Long confirmedRequests,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime eventDate,
        UserShortDto initiator,
        Boolean paid,
        String title,
        Long views
) {
}