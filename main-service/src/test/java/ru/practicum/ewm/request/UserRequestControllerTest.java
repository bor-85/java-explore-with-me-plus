package ru.practicum.ewm.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.client.StatClient;
import ru.practicum.ewm.controller.request.UserRequestController;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.request.Request;
import ru.practicum.ewm.model.request.RequestStatus;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.request.RequestRepository;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.ewm.service.request.UserRequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRequestController.class)
@Import(UserRequestService.class)
class UserRequestControllerTest {
    private static final User REQUESTER = new User(1L, "requester", "requester@mail.ru");
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 30, 12, 0, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private RequestRepository requestRepository;

    @MockBean
    private StatClient statClient;

    @ParameterizedTest
    @CsvSource({
            "true, 2, 1, PENDING",
            "false, 2, 1, CONFIRMED",
            "true, 0, 5, CONFIRMED",
            "false, 0, 5, CONFIRMED"
    })
    void createRequestReturns201WithSavedRequest(boolean requestModeration, int participantLimit,
                                                 long confirmedRequests, String expectedStatus) throws Exception {
        Event event = event(requestModeration, participantLimit);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(userRepository.getReferenceById(1L)).thenReturn(REQUESTER);
        when(requestRepository.countByEvent_IdAndStatus(2L, RequestStatus.CONFIRMED))
                .thenReturn(confirmedRequests);
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgument(0);
            request.setId(3L);
            return request;
        });
        LocalDateTime beforeCreation = LocalDateTime.now();

        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", "2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.event").value(2))
                .andExpect(jsonPath("$.requester").value(1))
                .andExpect(jsonPath("$.created").isNotEmpty())
                .andExpect(jsonPath("$.status").value(expectedStatus));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCreated()).isBetween(beforeCreation, LocalDateTime.now());
        verify(requestRepository).countByEvent_IdAndStatus(2L, RequestStatus.CONFIRMED);
    }

    @ParameterizedTest
    @CsvSource({"-1, 2", "abc, 2", "1, -1", "1, abc", "1, 9223372036854775808"})
    void createRequestRejectsInvalidIds(String userId, String eventId) throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", userId).param("eventId", eventId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verifyNoInteractions(userRepository, eventRepository, requestRepository);
    }

    @Test
    void createRequestRejectsMissingEventId() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository, eventRepository, requestRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void createRequestRejectsEmptyEventId(String eventId) throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", eventId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository, eventRepository, requestRepository);
    }

    @Test
    void createRequestReturns404WhenUserDoesNotExist() throws Exception {
        when(userRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(post("/users/{userId}/requests", 999L).param("eventId", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User with id=999 was not found"));

        verifyNoInteractions(eventRepository, requestRepository);
    }

    @Test
    void createRequestReturns404WhenEventDoesNotExist() throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Event with id=999 was not found"));

        verifyNoInteractions(requestRepository);
    }

    @Test
    void createRequestReturns409WhenUserIsEventInitiator() throws Exception {
        Event event = event(true, 10);
        event.setInitiator(REQUESTER);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));

        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("You cannot join an event you created"));

        verifyNoInteractions(requestRepository);
    }

    @ParameterizedTest
    @EnumSource(value = EventState.class, names = {"PENDING", "CANCELED"})
    void createRequestReturns409WhenEventIsNotPublished(EventState state) throws Exception {
        Event event = event(true, 10);
        event.setState(state);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));

        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value("Event status is not PUBLISHED"));

        verifyNoInteractions(requestRepository);
    }

    @Test
    void createRequestReturns409WhenRequestAlreadyExists() throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event(true, 10)));
        when(requestRepository.existsByEvent_IdAndRequester_Id(2L, 1L)).thenReturn(true);

        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("User with id=1 has already been sent request for event with id=2"));

        verify(requestRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"true, 1", "true, 2", "false, 1", "false, 2"})
    void createRequestReturns409WhenParticipantLimitReached(boolean requestModeration,
                                                           long confirmedRequests) throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event(requestModeration, 1)));
        when(requestRepository.countByEvent_IdAndStatus(2L, RequestStatus.CONFIRMED))
                .thenReturn(confirmedRequests);

        mockMvc.perform(post("/users/{userId}/requests", 1L).param("eventId", "2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("The participant limit for this event has been reached"));

        verify(requestRepository, never()).save(any());
    }

    @Test
    void getRequestsReturnsUserRequestsWithAllFields() throws Exception {
        Request first = request(3L, event(true, 10), RequestStatus.PENDING);
        Event anotherEvent = event(false, 0);
        anotherEvent.setId(5L);
        Request second = request(4L, anotherEvent, RequestStatus.CANCELED);
        second.setCreated(CREATED.plusMinutes(1));
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findAllByRequester_Id(1L)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/users/{userId}/requests", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].event").value(2))
                .andExpect(jsonPath("$[0].requester").value(1))
                .andExpect(jsonPath("$[0].created").value(CREATED.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(4))
                .andExpect(jsonPath("$[1].event").value(5))
                .andExpect(jsonPath("$[1].requester").value(1))
                .andExpect(jsonPath("$[1].created").value(CREATED.plusMinutes(1).toString()))
                .andExpect(jsonPath("$[1].status").value("CANCELED"));

        verify(requestRepository).findAllByRequester_Id(1L);
        verify(requestRepository, never()).save(any());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void getRequestsReturnsEmptyListWhenUserHasNoRequests() throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findAllByRequester_Id(1L)).thenReturn(List.of());

        mockMvc.perform(get("/users/{userId}/requests", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(requestRepository).findAllByRequester_Id(1L);
    }

    @Test
    void getRequestsReturns404WhenUserDoesNotExist() throws Exception {
        when(userRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(get("/users/{userId}/requests", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User with id=999 was not found"));

        verifyNoInteractions(eventRepository, requestRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc"})
    void getRequestsRejectsInvalidUserId(String userId) throws Exception {
        mockMvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository, eventRepository, requestRepository);
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"PENDING", "CONFIRMED", "CANCELED"})
    void cancelRequestReturns200WithCanceledRequest(RequestStatus initialStatus) throws Exception {
        Request request = request(3L, event(true, 10), initialStatus);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findByIdAndRequester_Id(3L, 1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 1L, 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.event").value(2))
                .andExpect(jsonPath("$.requester").value(1))
                .andExpect(jsonPath("$.created").value(CREATED.toString()))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(requestRepository).findByIdAndRequester_Id(3L, 1L);
        verify(requestRepository).save(request);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELED);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void cancelRequestReturns404WhenUserDoesNotExist() throws Exception {
        when(userRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 999L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User with id=999 was not found"));

        verifyNoInteractions(eventRepository, requestRepository);
    }

    @Test
    void cancelRequestReturns404WhenRequestDoesNotExist() throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findByIdAndRequester_Id(999L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 1L, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Request with id=999 not found for user with id=1"));

        verify(requestRepository, never()).save(any());
    }

    @Test
    void cancelRequestReturns404WhenRequestBelongsToAnotherUser() throws Exception {
        when(userRepository.existsById(4L)).thenReturn(true);
        when(requestRepository.findByIdAndRequester_Id(3L, 4L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 4L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Request with id=3 not found for user with id=4"));

        verify(requestRepository).findByIdAndRequester_Id(3L, 4L);
        verify(requestRepository, never()).findById(anyLong());
        verify(requestRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"-1, 3", "abc, 3", "1, -1", "1, abc"})
    void cancelRequestRejectsInvalidIds(String userId, String requestId) throws Exception {
        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(userRepository, eventRepository, requestRepository);
    }

    private Event event(boolean requestModeration, int participantLimit) {
        Event event = new Event();
        event.setId(2L);
        event.setInitiator(new User(10L, "initiator", "initiator@mail.ru"));
        event.setState(EventState.PUBLISHED);
        event.setRequestModeration(requestModeration);
        event.setParticipantLimit(participantLimit);
        return event;
    }

    private Request request(Long id, Event event, RequestStatus status) {
        return Request.builder()
                .id(id)
                .event(event)
                .requester(REQUESTER)
                .created(CREATED)
                .status(status)
                .build();
    }
}
