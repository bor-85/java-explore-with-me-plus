package ru.practicum.ewm.compilation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.compilation.CompilationMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.compilation.Compilation;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.repository.compilation.CompilationRepository;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.service.compilation.CompilationServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {
    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Spy
    private CompilationMapper compilationMapper = new CompilationMapper();

    @InjectMocks
    private CompilationServiceImpl compilationService;

    @Test
    void createCompilationCreatesCompilationWithEvents() {
        NewCompilationDto dto = new NewCompilationDto(List.of(1L), false, "Summer");

        Event event = buildEvent(1L);

        when(compilationRepository.findByTitleIgnoreCase("Summer"))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(compilationRepository.save(any()))
                .thenAnswer(invocation -> {
                    Compilation compilation = invocation.getArgument(0);
                    compilation.setId(1L);
                    return compilation;
                });

        CompilationDto result = compilationService.createCompilation(dto);

        assertEquals(1L, result.id());
        assertEquals(false, result.pinned());
        assertEquals("Summer", result.title());
        assertEquals(1, result.events().size());
        assertEquals(1L, result.events().get(0).id());
        assertEquals("Event 1", result.events().get(0).title());

        verify(compilationRepository).save(any());
    }

    @Test
    void createCompilationThrowsConflictWhenTitleExists() {
        NewCompilationDto dto = new NewCompilationDto(List.of(1L), false, "Summer");

        Compilation existing = buildCompilation(2L, "Summer", false, List.of());

        when(compilationRepository.findByTitleIgnoreCase("Summer"))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> compilationService.createCompilation(dto));
    }

    @Test
    void updateCompilationUpdatesCompilation() {
        Compilation existing = buildCompilation(1L, "Old", false, List.of());
        UpdateCompilationRequest dto = new UpdateCompilationRequest(List.of(1L), true, "Updated");
        Event event = buildEvent(1L);

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(existing));
        when(compilationRepository.findByTitleIgnoreCase("Updated"))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(compilationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompilationDto result = compilationService.updateCompilation(1L, dto);

        assertEquals(1L, result.id());
        assertEquals(true, result.pinned());
        assertEquals("Updated", result.title());
        assertEquals(1, result.events().size());
        assertEquals(1L, result.events().get(0).id());

        verify(compilationRepository).save(any());
    }

    @Test
    void updateCompilationThrowsConflictWhenTitleExists() {
        Compilation existing = buildCompilation(1L, "Old", false, List.of());
        UpdateCompilationRequest dto = new UpdateCompilationRequest(null, true, "Summer");
        Compilation other = buildCompilation(2L, "Summer", false, List.of());

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(existing));
        when(compilationRepository.findByTitleIgnoreCase("Summer"))
                .thenReturn(Optional.of(other));

        assertThrows(ConflictException.class, () -> compilationService.updateCompilation(1L, dto));
    }

    @Test
    void updateCompilationThrowsNotFoundWhenCompilationMissing() {
        UpdateCompilationRequest dto = new UpdateCompilationRequest(List.of(1L), true, "Updated");

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.updateCompilation(1L, dto));
    }

    @Test
    void deleteCompilationDeletesExistingCompilation() {
        Compilation existing = buildCompilation(1L, "Summer", false, List.of());

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        compilationService.deleteCompilation(1L);

        verify(compilationRepository).delete(existing);
    }

    @Test
    void deleteCompilationThrowsNotFoundWhenCompilationMissing() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.deleteCompilation(1L));
    }

    @Test
    void getCompilationsReturnsPinnedFilteredCompilations() {
        Compilation compilation = buildCompilation(1L, "Pinned", true, List.of(buildEvent(1L)));

        when(compilationRepository.findByPinnedOrderByIdAsc(true))
                .thenReturn(List.of(compilation));

        List<CompilationDto> result = compilationService.getCompilations(true, 0, 10);

        assertEquals(1, result.size());
        assertTrue(result.get(0).pinned());
        assertEquals("Pinned", result.get(0).title());
        assertEquals(1, result.get(0).events().size());
    }

    @Test
    void getCompilationsReturnsEmptyListWhenFromIsOutOfRange() {
        Compilation compilation = buildCompilation(1L, "Pinned", true, List.of());

        when(compilationRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(compilation));

        List<CompilationDto> result = compilationService.getCompilations(null, 10, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCompilationReturnsDto() {
        Compilation compilation = buildCompilation(1L, "Summer", false, List.of(buildEvent(1L)));

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        CompilationDto result = compilationService.getCompilation(1L);

        assertEquals(1L, result.id());
        assertEquals("Summer", result.title());
        assertEquals(1, result.events().size());
    }

    @Test
    void getCompilationThrowsNotFoundWhenCompilationMissing() {
        when(compilationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> compilationService.getCompilation(1L));
    }

    private Compilation buildCompilation(Long id, String title, boolean pinned, List<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setId(id);
        compilation.setTitle(title);
        compilation.setPinned(pinned);
        compilation.setEvents(new ArrayList<>(events));
        return compilation;
    }

    private Event buildEvent(Long id) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Music");

        User initiator = new User();
        initiator.setId(2L);
        initiator.setName("Ivan");

        Event event = new Event();
        event.setId(id);
        event.setAnnotation("Annotation " + id);
        event.setCategory(category);
        event.setDescription("Description " + id);
        event.setEventDate(LocalDateTime.of(2024, 1, 1, 12, 0));
        event.setInitiator(initiator);
        event.setLat(55.0);
        event.setLon(37.0);
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setState(EventState.PENDING);
        event.setTitle("Event " + id);
        event.setCreatedOn(LocalDateTime.of(2024, 1, 1, 10, 0));
        return event;
    }
}
