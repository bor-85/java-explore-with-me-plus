package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.ewm.service.event.EventService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEventThrowsWhenEventDateEarlierThanTwoHours() {
        NewEventDto dto = newEventDto(LocalDateTime.now().plusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Пользователь", "user@mail.ru")));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "Концерты")));

        assertThatThrownBy(() -> eventService.createEvent(1L, dto))
                .isInstanceOf(ValidationException.class);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateUserEventThrowsConflictWhenEventPublished() {
        Event event = event(EventState.PUBLISHED);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, updateRequest("CANCEL_REVIEW")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateUserEventCancelsReview() {
        Event event = event(EventState.PENDING);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        EventFullDto result = eventService.updateUserEvent(1L, 1L, updateRequest("CANCEL_REVIEW"));

        assertThat(result.state()).isEqualTo("CANCELED");
        assertThat(result.title()).isEqualTo("Новое название");
    }

    private NewEventDto newEventDto(LocalDateTime eventDate) {
        return new NewEventDto(
                "Аннотация тестового события",
                1L,
                "Описание тестового события",
                eventDate,
                new Location(55.75, 37.62),
                null,
                null,
                null,
                "Название"
        );
    }

    private UpdateEventUserRequest updateRequest(String stateAction) {
        return new UpdateEventUserRequest(null, null, null, null, null, null, null, null, stateAction, "Новое название");
    }

    private Event event(EventState state) {
        Event event = new Event();
        event.setId(1L);
        event.setAnnotation("Аннотация тестового события");
        event.setCategory(new Category(1L, "Концерты"));
        event.setDescription("Описание тестового события");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setInitiator(new User(1L, "Пользователь", "user@mail.ru"));
        event.setLat(55.75);
        event.setLon(37.62);
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setState(state);
        event.setTitle("Название");
        event.setCreatedOn(LocalDateTime.now());
        return event;
    }
}
