package ru.practicum.ewm.mapper.compilation;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class CompilationMapper {
    public CompilationDto toDto(Compilation compilation) {
        return toDto(compilation, Map.of(), Map.of());
    }

    public CompilationDto toDto(Compilation compilation,
                                Map<Long, Long> confirmedRequestsByEvent,
                                Map<Long, Long> viewsByEvent) {
        List<EventShortDto> events = compilation.getEvents() == null
                ? List.of()
                : compilation.getEvents().stream()
                .sorted(Comparator.comparing(Event::getId))
                .map(event -> toEventShortDto(
                        event,
                        confirmedRequestsByEvent.getOrDefault(event.getId(), 0L),
                        viewsByEvent.getOrDefault(event.getId(), 0L)
                ))
                .toList();

        return new CompilationDto(
                compilation.getId(),
                compilation.isPinned(),
                compilation.getTitle(),
                events
        );
    }

    public Compilation toEntity(NewCompilationDto dto, List<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.title());
        compilation.setPinned(dto.pinned() != null && dto.pinned());
        compilation.setEvents(new ArrayList<>(events));
        return compilation;
    }

    public void update(Compilation compilation, UpdateCompilationRequest dto, List<Event> events) {
        if (dto.title() != null) {
            compilation.setTitle(dto.title());
        }
        if (dto.pinned() != null) {
            compilation.setPinned(dto.pinned());
        }
        if (events != null) {
            compilation.getEvents().clear();
            compilation.getEvents().addAll(events);
        }
    }

    public EventShortDto toEventShortDto(Event event, long confirmedRequests, long views) {
        return new EventShortDto(
                event.getId(),
                event.getAnnotation(),
                new CategoryDto(event.getCategory().getId(), event.getCategory().getName()),
                confirmedRequests,
                event.getEventDate(),
                new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()),
                event.getPaid(),
                event.getTitle(),
                views
        );
    }
}