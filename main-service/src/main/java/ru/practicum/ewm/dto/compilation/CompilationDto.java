package ru.practicum.ewm.dto.compilation;

import ru.practicum.ewm.dto.event.EventShortDto;

import java.util.List;

public record CompilationDto(
        Long id,
        Boolean pinned,
        String title,
        List<EventShortDto> events
) {
}