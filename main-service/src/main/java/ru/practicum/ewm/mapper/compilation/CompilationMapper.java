package ru.practicum.ewm.mapper.compilation;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation) {
        List<ru.practicum.ewm.dto.event.EventShortDto> events = compilation.getEvents() == null
                ? List.of()
                : compilation.getEvents().stream()
                .map(event -> EventMapper.toShortDto(event, 0L, 0L))
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
}