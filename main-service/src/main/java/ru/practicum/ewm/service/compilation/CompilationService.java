package ru.practicum.ewm.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.compilation.CompilationMapper;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.repository.compilation.CompilationRepository;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.service.event.EventService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.ewm.exception.ErrorMessages.COMPILATION_ALREADY_EXISTS;
import static ru.practicum.ewm.exception.ErrorMessages.COMPILATION_NOT_FOUND;
import static ru.practicum.ewm.exception.ErrorMessages.EVENT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventService eventService;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        checkTitleUnique(newCompilationDto.title(), null);

        List<Event> events = loadEvents(newCompilationDto.events());
        Compilation compilation = compilationMapper.toEntity(newCompilationDto, events);

        return toDto(compilationRepository.save(compilation));
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = getCompilationOrThrow(compId);

        if (updateCompilationRequest.title() != null) {
            checkTitleUnique(updateCompilationRequest.title(), compId);
        }

        List<Event> events = updateCompilationRequest.events() == null
                ? null
                : loadEvents(updateCompilationRequest.events());

        compilationMapper.update(compilation, updateCompilationRequest, events);

        return toDto(compilation);
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        compilationRepository.delete(compilation);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        List<Compilation> page = compilationRepository.findAllPaged(pinned, from, size);

        if (page.isEmpty()) {
            return List.of();
        }

        List<Long> compilationIds = page.stream()
                .map(Compilation::getId)
                .toList();

        List<Compilation> compilationsWithEvents =
                compilationRepository.findAllWithEventsByIdIn(compilationIds);

        Map<Long, Compilation> compilationsById = compilationsWithEvents.stream()
                .collect(Collectors.toMap(Compilation::getId, Function.identity()));

        // IN-запрос не гарантирует порядок, поэтому восстанавливаем порядок страницы.
        List<Compilation> orderedCompilations = compilationIds.stream()
                .map(compilationsById::get)
                .toList();

        Map<Long, Event> uniqueEvents = orderedCompilations.stream()
                .flatMap(compilation -> getEvents(compilation).stream())
                .collect(Collectors.toMap(
                        Event::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        Map<Long, EventShortDto> eventDtosById;
        if (uniqueEvents.isEmpty()) {
            eventDtosById = Map.of();
        } else {
            List<EventShortDto> eventDtos = eventService.toShortDtos(
                    new ArrayList<>(uniqueEvents.values())
            );

            eventDtosById = eventDtos.stream()
                    .collect(Collectors.toMap(EventShortDto::id, Function.identity()));
        }

        return orderedCompilations.stream()
                .map(compilation -> {
                    List<EventShortDto> eventDtos = getEvents(compilation).stream()
                            .map(event -> eventDtosById.get(event.getId()))
                            .toList();

                    return compilationMapper.toDto(compilation, eventDtos);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = compilationRepository.findWithEventsById(compId)
                .orElseThrow(() -> new NotFoundException(
                        COMPILATION_NOT_FOUND.formatted(compId)
                ));

        return toDto(compilation);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> eventDtos = getShortDtos(getEvents(compilation));
        return compilationMapper.toDto(compilation, eventDtos);
    }

    private List<EventShortDto> getShortDtos(List<Event> events) {
        return events.isEmpty() ? List.of() : eventService.toShortDtos(events);
    }

    private List<Event> getEvents(Compilation compilation) {
        return compilation.getEvents() == null ? List.of() : compilation.getEvents();
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(
                        COMPILATION_NOT_FOUND.formatted(compId)
                ));
    }

    private void checkTitleUnique(String title, Long currentId) {
        boolean exists = currentId == null
                ? compilationRepository.existsByTitle(title)
                : compilationRepository.existsByTitleAndIdNot(title, currentId);

        if (exists) {
            throw new ConflictException(COMPILATION_ALREADY_EXISTS.formatted(title));
        }
    }

    private List<Event> loadEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = new LinkedHashSet<>(eventIds);
        List<Event> events = eventRepository.findAllById(ids);

        Map<Long, Event> eventsById = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        if (eventsById.size() != ids.size()) {
            Long missingId = ids.stream()
                    .filter(id -> !eventsById.containsKey(id))
                    .findFirst()
                    .orElseThrow();

            throw new NotFoundException(EVENT_NOT_FOUND.formatted(missingId));
        }

        return ids.stream()
                .map(eventsById::get)
                .toList();
    }
}