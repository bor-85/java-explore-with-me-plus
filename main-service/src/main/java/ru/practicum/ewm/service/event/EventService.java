package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.event.UpdateEventRequest;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ErrorMessages;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.event.EventMapper;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.repository.category.CategoryRepository;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.user.UserRepository;
import ru.practicum.client.StatClient;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final long MIN_HOURS_BEFORE_EVENT = 2;
    private static final long MIN_HOURS_BEFORE_PUBLICATION = 1;
    private static final LocalDateTime STATS_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final String EVENT_URI_PREFIX = "/events/";

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatClient statClient;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId)));
        Category category = getCategory(dto.category());
        checkEventDate(dto.eventDate(), MIN_HOURS_BEFORE_EVENT);

        Event event = EventMapper.toEvent(dto, initiator, category);
        // new event has no requests and no views yet
        return EventMapper.toFullDto(eventRepository.save(event), 0L, 0L);
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        return toShortDtos(eventRepository.findAllByInitiatorId(userId, from, size));
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        return toFullDto(getUserEventEntity(userId, eventId));
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getUserEventEntity(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException(ErrorMessages.EVENT_NOT_EDITABLE);
        }
        if (dto.eventDate() != null) {
            checkEventDate(dto.eventDate(), MIN_HOURS_BEFORE_EVENT);
        }
        updateFields(event, dto);
        if ("SEND_TO_REVIEW".equals(dto.stateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(dto.stateAction())) {
            event.setState(EventState.CANCELED);
        }

        return toFullDto(event);
    }

    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException(ErrorMessages.RANGE_START_AFTER_END);
        }
        return toFullDtos(
                eventRepository.findAllByAdminFilters(users, states, categories, rangeStart, rangeEnd, from, size));
    }

    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               boolean onlyAvailable, String sort, int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException(ErrorMessages.RANGE_START_AFTER_END);
        }
        String pattern = text != null ? "%" + text.toLowerCase() + "%" : null;
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();

        List<Event> events = eventRepository.findAllPublished(pattern, categories, paid, start, rangeEnd);
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);

        Stream<Event> stream = events.stream();
        if (onlyAvailable) {
            stream = stream.filter(e -> e.getParticipantLimit() == 0
                    || confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit());
        }
        if ("VIEWS".equals(sort)) {
            stream = stream.sorted(Comparator.comparing((Event e) -> views.getOrDefault(e.getId(), 0L)).reversed());
        }
        return stream.skip(from)
                .limit(size)
                .map(e -> EventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public EventFullDto getPublicEvent(Long eventId) {
        return toFullDto(eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.EVENT_NOT_FOUND.formatted(eventId))));
    }

    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.EVENT_NOT_FOUND.formatted(eventId)));

        if (dto.eventDate() != null) {
            checkEventDate(dto.eventDate(), MIN_HOURS_BEFORE_PUBLICATION);
        }
        if ("PUBLISH_EVENT".equals(dto.stateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException(ErrorMessages.EVENT_NOT_PENDING.formatted(event.getState()));
            }
            LocalDateTime eventDate = dto.eventDate() != null ? dto.eventDate() : event.getEventDate();
            LocalDateTime now = LocalDateTime.now();
            if (eventDate.isBefore(now.plusHours(MIN_HOURS_BEFORE_PUBLICATION))) {
                throw new ConflictException(
                        ErrorMessages.EVENT_PUBLISH_TOO_LATE.formatted(MIN_HOURS_BEFORE_PUBLICATION, eventDate));
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(now);
        } else if ("REJECT_EVENT".equals(dto.stateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException(ErrorMessages.EVENT_ALREADY_PUBLISHED);
            }
            event.setState(EventState.CANCELED);
        }
        updateFields(event, dto);

        return toFullDto(event);
    }

    private EventFullDto toFullDto(Event event) {
        return toFullDtos(List.of(event)).get(0);
    }

    private List<EventFullDto> toFullDtos(List<Event> events) {
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);
        return events.stream()
                .map(e -> EventMapper.toFullDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public List<EventShortDto> toShortDtos(List<Event> events) {
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        return eventRepository.countConfirmedRequests(events.stream().map(Event::getId).toList())
                .stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()));
    }

    private Map<Long, Long> getViews(List<Event> events) {
        List<String> uris = events.stream()
                .filter(e -> e.getPublishedOn() != null)
                .map(e -> EVENT_URI_PREFIX + e.getId())
                .toList();
        if (uris.isEmpty()) {
            return Map.of();
        }
        return statClient.getStats(STATS_START, LocalDateTime.now(), uris, true)
                .stream()
                .collect(Collectors.toMap(stat -> Long.parseLong(stat.uri().substring(EVENT_URI_PREFIX.length())),
                        ViewStatsDto::hits, Long::sum));
    }

    private void updateFields(Event event, UpdateEventRequest dto) {
        if (dto.eventDate() != null) {
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
    }

    private Event getUserEventEntity(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.EVENT_NOT_FOUND.formatted(eventId)));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.CATEGORY_NOT_FOUND.formatted(categoryId)));
    }

    private void checkEventDate(LocalDateTime eventDate, long minHours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(minHours))) {
            throw new ValidationException(ErrorMessages.EVENT_DATE_TOO_EARLY.formatted(minHours, eventDate));
        }
    }
}
