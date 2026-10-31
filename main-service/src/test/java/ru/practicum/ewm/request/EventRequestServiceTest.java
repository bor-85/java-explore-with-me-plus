package ru.practicum.ewm.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.request.Request;
import ru.practicum.ewm.model.request.RequestStatus;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.request.RequestRepository;
import ru.practicum.ewm.service.request.UserRequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRequestServiceTest {
    private static final long USER_ID = 10L;
    private static final long EVENT_ID = 2L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private UserRequestService requestService;

    @Test
    void getEventRequestsThrowsWhenUserIsNotInitiator() {
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.getEventRequests(USER_ID, EVENT_ID));
    }

    @Test
    void confirmRejectsOverflowAndRemainingPendingWhenLimitReached() {
        Event event = event(2);
        Request first = request(1L, event, RequestStatus.PENDING);
        Request second = request(2L, event, RequestStatus.PENDING);
        Request other = request(3L, event, RequestStatus.PENDING);
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByEvent_IdAndIdIn(EVENT_ID, List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(requestRepository.countByEvent_IdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(1L);
        when(requestRepository.findAllByEvent_IdAndStatus(EVENT_ID, RequestStatus.PENDING)).thenReturn(List.of(other));

        EventRequestStatusUpdateResult result = requestService.updateEventRequests(USER_ID, EVENT_ID,
                new EventRequestStatusUpdateRequest(List.of(1L, 2L), "CONFIRMED"));

        assertEquals(1, result.confirmedRequests().size());
        assertEquals(1, result.rejectedRequests().size());
        assertEquals(RequestStatus.CONFIRMED, first.getStatus());
        assertEquals(RequestStatus.REJECTED, second.getStatus());
        assertEquals(RequestStatus.REJECTED, other.getStatus());
    }

    @Test
    void confirmThrowsWhenLimitAlreadyReached() {
        Event event = event(1);
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByEvent_IdAndIdIn(EVENT_ID, List.of(1L)))
                .thenReturn(List.of(request(1L, event, RequestStatus.PENDING)));
        when(requestRepository.countByEvent_IdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> requestService.updateEventRequests(USER_ID, EVENT_ID,
                new EventRequestStatusUpdateRequest(List.of(1L), "CONFIRMED")));
    }

    @Test
    void updateThrowsWhenRequestIsNotPending() {
        Event event = event(0);
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByEvent_IdAndIdIn(EVENT_ID, List.of(1L)))
                .thenReturn(List.of(request(1L, event, RequestStatus.CONFIRMED)));

        assertThrows(ConflictException.class, () -> requestService.updateEventRequests(USER_ID, EVENT_ID,
                new EventRequestStatusUpdateRequest(List.of(1L), "REJECTED")));
    }

    @Test
    void updateThrowsWhenRequestNotFoundForEvent() {
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event(0)));
        when(requestRepository.findAllByEvent_IdAndIdIn(EVENT_ID, List.of(1L))).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> requestService.updateEventRequests(USER_ID, EVENT_ID,
                new EventRequestStatusUpdateRequest(List.of(1L), "REJECTED")));
    }

    private Event event(int participantLimit) {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setRequestModeration(true);
        event.setParticipantLimit(participantLimit);
        return event;
    }

    private Request request(Long id, Event event, RequestStatus status) {
        return Request.builder()
                .id(id)
                .event(event)
                .requester(new User(1L, "requester", "requester@mail.ru"))
                .created(LocalDateTime.of(2026, 10, 30, 12, 0))
                .status(status)
                .build();
    }
}
