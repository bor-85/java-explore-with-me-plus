package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final long MIN_HOURS_BEFORE_EVENT = 2;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = getCategory(dto.category());
        checkEventDate(dto.eventDate());

        Event event = EventMapper.toEvent(dto, initiator, category);
        return EventMapper.toFullDto(eventRepository.save(event));
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        return eventRepository.findAllByInitiatorId(userId, from, size)
                .stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        return EventMapper.toFullDto(getUserEventEntity(userId, eventId));
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getUserEventEntity(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (dto.eventDate() != null) {
            checkEventDate(dto.eventDate());
            event.setEventDate(dto.eventDate());
        }
        if (dto.annotation() != null) {
            event.setAnnotation(dto.annotation());
        }
        if (dto.category() != null) {
            event.setCategory(getCategory(dto.category()));
        }
        if (dto.description() != null) {
            event.setDescription(dto.description());
        }
        if (dto.location() != null) {
            event.setLat(dto.location().lat());
            event.setLon(dto.location().lon());
        }
        if (dto.paid() != null) {
            event.setPaid(dto.paid());
        }
        if (dto.participantLimit() != null) {
            event.setParticipantLimit(dto.participantLimit());
        }
        if (dto.requestModeration() != null) {
            event.setRequestModeration(dto.requestModeration());
        }
        if (dto.title() != null) {
            event.setTitle(dto.title());
        }
        if ("SEND_TO_REVIEW".equals(dto.stateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(dto.stateAction())) {
            event.setState(EventState.CANCELED);
        }

        return EventMapper.toFullDto(event);
    }

    private Event getUserEventEntity(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
    }

    private void checkEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new IllegalArgumentException(
                    "Event date must be at least " + MIN_HOURS_BEFORE_EVENT + " hours from now: " + eventDate);
        }
    }
}
