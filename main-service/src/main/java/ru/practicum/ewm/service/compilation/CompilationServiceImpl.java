package ru.practicum.ewm.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.compilation.CompilationMapper;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.repository.compilation.CompilationRepository;
import ru.practicum.ewm.repository.event.EventRepository;

import java.util.LinkedHashSet;
import java.util.List;

import static ru.practicum.ewm.exception.ErrorMessages.*;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        checkTitleUnique(newCompilationDto.title(), null);

        List<Event> events = loadEvents(newCompilationDto.events());
        Compilation compilation = compilationMapper.toEntity(newCompilationDto, events);

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }

    @Override
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

        return compilationMapper.toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        List<Compilation> compilations = pinned == null
                ? compilationRepository.findAllByOrderByIdAsc()
                : compilationRepository.findByPinnedOrderByIdAsc(pinned);

        if (from >= compilations.size()) {
            return List.of();
        }

        int toIndex = Math.min(from + size, compilations.size());

        return compilations.subList(from, toIndex).stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compId) {
        return compilationMapper.toDto(getCompilationOrThrow(compId));
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(
                        COMPILATION_NOT_FOUND.formatted(compId)
                ));
    }

    private void checkTitleUnique(String title, Long currentId) {
        compilationRepository.findByTitleIgnoreCase(title)
                .ifPresent(compilation -> {
                    if (currentId == null || !compilation.getId().equals(currentId)) {
                        throw new ConflictException(
                                COMPILATION_ALREADY_EXISTS.formatted(title)
                        );
                    }
                });
    }

    private List<Event> loadEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        return new LinkedHashSet<>(eventIds).stream()
                .map(this::getEventOrThrow)
                .toList();
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        EVENT_NOT_FOUND.formatted(eventId)
                ));
    }
}