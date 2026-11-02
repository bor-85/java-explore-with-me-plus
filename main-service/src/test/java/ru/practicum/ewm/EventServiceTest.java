package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.client.StatClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.category.CategoryRepository;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.ewm.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StatClient statClient;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEventThrowsWhenEventDateEarlierThanTwoHours() {
        NewEventDto dto = newEventDto(LocalDateTime.now().plusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "user", "user@mail.ru")));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new Category(1L, "concerts")));

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
        assertThat(result.title()).isEqualTo("New title");
    }

    @Test
    void updateAdminEventPublishesPendingEvent() {
        Event event = event(EventState.PENDING);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventFullDto result = eventService.updateAdminEvent(1L, adminRequest("PUBLISH_EVENT"));

        assertThat(result.state()).isEqualTo("PUBLISHED");
        assertThat(result.publishedOn()).isNotNull();
    }

    @Test
    void updateAdminEventThrowsConflictWhenPublishingNotPendingEvent() {
        Event event = event(EventState.CANCELED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, adminRequest("PUBLISH_EVENT")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateAdminEventThrowsConflictWhenPublishingLessThanHourBeforeEvent() {
        Event event = event(EventState.PENDING);
        event.setEventDate(LocalDateTime.now().plusMinutes(30));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, adminRequest("PUBLISH_EVENT")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateAdminEventThrowsConflictWhenRejectingPublishedEvent() {
        Event event = event(EventState.PUBLISHED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateAdminEvent(1L, adminRequest("REJECT_EVENT")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getPublicEventThrowsNotFoundWhenEventNotPublished() {
        when(eventRepository.findByIdAndState(1L, EventState.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublicEvent(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPublicEventsSortsByViewsAndSkipsEventsWithoutFreePlaces() {
        Event first = publishedEvent(1L, 0);
        Event second = publishedEvent(2L, 0);
        Event full = publishedEvent(3L, 1);
        when(eventRepository.findAllPublished(any(), any(), any(), any(), any()))
                .thenReturn(List.of(first, second, full));
        when(eventRepository.countConfirmedRequests(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{3L, 1L}));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 5L),
                new ViewStatsDto("ewm-main-service", "/events/2", 10L),
                new ViewStatsDto("ewm-main-service", "/events/3", 20L)));

        List<EventShortDto> result = eventService.getPublicEvents(null, null, null, null, null,
                true, "VIEWS", 0, 10);

        assertThat(result).extracting(EventShortDto::id).containsExactly(2L, 1L);
        assertThat(result).extracting(EventShortDto::views).containsExactly(10L, 5L);
    }

    @Test
    void toShortDtosMatchesStatisticsByEventIdAndSumsViews() {
        Event first = publishedEvent(1L, 0);
        Event second = publishedEvent(2L, 0);
        Event withoutStatistics = publishedEvent(3L, 0);
        Event unpublished = event(EventState.PENDING);
        unpublished.setId(4L);
        when(eventRepository.countConfirmedRequests(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(new Object[]{2L, 4L}, new Object[]{1L, 2L}));
        when(statClient.getStats(any(), any(), eq(List.of("/events/1", "/events/2", "/events/3")), eq(true)))
                .thenReturn(List.of(
                        new ViewStatsDto("ewm-main-service", "/events/2", 7L),
                        new ViewStatsDto("ewm-main-service", "/events/1", 5L),
                        new ViewStatsDto("ewm-main-service", "/events/2", 3L)));

        List<EventShortDto> result = eventService.toShortDtos(List.of(first, second, withoutStatistics, unpublished));

        assertThat(result)
                .extracting(EventShortDto::id, EventShortDto::confirmedRequests, EventShortDto::views)
                .containsExactly(tuple(1L, 2L, 5L), tuple(2L, 4L, 10L), tuple(3L, 0L, 0L), tuple(4L, 0L, 0L));
        verify(eventRepository).countConfirmedRequests(List.of(1L, 2L, 3L, 4L));
        verify(statClient).getStats(eq(LocalDateTime.of(2000, 1, 1, 0, 0)), any(),
                eq(List.of("/events/1", "/events/2", "/events/3")), eq(true));
    }

    @Test
    void toShortDtosSkipsViewsForUnpublishedEvents() {
        Event event = event(EventState.PENDING);
        when(eventRepository.countConfirmedRequests(List.of(1L)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));

        List<EventShortDto> result = eventService.toShortDtos(List.of(event));

        assertThat(result)
                .extracting(EventShortDto::id, EventShortDto::confirmedRequests, EventShortDto::views)
                .containsExactly(tuple(1L, 2L, 0L));
        verifyNoInteractions(statClient);
    }

    @Test
    void toShortDtosSkipsStatisticsForEmptyEvents() {
        assertThat(eventService.toShortDtos(List.of())).isEmpty();

        verifyNoInteractions(eventRepository, statClient);
    }

    private Event publishedEvent(Long id, int participantLimit) {
        Event event = event(EventState.PUBLISHED);
        event.setId(id);
        event.setParticipantLimit(participantLimit);
        event.setPublishedOn(LocalDateTime.now());
        return event;
    }

    private UpdateEventAdminRequest adminRequest(String stateAction) {
        return new UpdateEventAdminRequest(null, null, null, null, null, null, null, null, stateAction, null);
    }

    private NewEventDto newEventDto(LocalDateTime eventDate) {
        return new NewEventDto(
                "Annotation of the test event",
                1L,
                "Description of the test event",
                eventDate,
                new Location(55.75, 37.62),
                null,
                null,
                null,
                "Title"
        );
    }

    private UpdateEventUserRequest updateRequest(String stateAction) {
        return new UpdateEventUserRequest(null, null, null, null, null, null, null, null, stateAction, "New title");
    }

    private Event event(EventState state) {
        Event event = new Event();
        event.setId(1L);
        event.setAnnotation("Annotation of the test event");
        event.setCategory(new Category(1L, "concerts"));
        event.setDescription("Description of the test event");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setInitiator(new User(1L, "user", "user@mail.ru"));
        event.setLat(55.75);
        event.setLon(37.62);
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setState(state);
        event.setTitle("Title");
        event.setCreatedOn(LocalDateTime.now());
        return event;
    }
}
