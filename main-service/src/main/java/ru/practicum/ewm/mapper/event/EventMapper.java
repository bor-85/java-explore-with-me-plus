package ru.practicum.ewm.mapper.event;

import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.model.category.Category;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;

import java.time.LocalDateTime;

public final class EventMapper {

    private EventMapper() {
    }

    public static Event toEvent(NewEventDto dto, User initiator, Category category) {
        Event event = new Event();
        event.setAnnotation(dto.annotation());
        event.setCategory(category);
        event.setDescription(dto.description());
        event.setEventDate(dto.eventDate());
        event.setInitiator(initiator);
        event.setLat(dto.location().lat());
        event.setLon(dto.location().lon());
        event.setPaid(dto.paid() != null ? dto.paid() : false);
        event.setParticipantLimit(dto.participantLimit() != null ? dto.participantLimit() : 0);
        event.setRequestModeration(dto.requestModeration() != null ? dto.requestModeration() : true);
        event.setState(EventState.PENDING);
        event.setTitle(dto.title());
        event.setCreatedOn(LocalDateTime.now());
        return event;
    }

    public static EventFullDto toFullDto(Event event, long confirmedRequests, long views) {
        return new EventFullDto(
                event.getId(),
                event.getAnnotation(),
                toCategoryDto(event.getCategory()),
                confirmedRequests,
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                UserMapper.toShortDto(event.getInitiator()),
                new Location(event.getLat(), event.getLon()),
                event.getPaid(),
                event.getParticipantLimit(),
                event.getPublishedOn(),
                event.getRequestModeration(),
                event.getState().name(),
                event.getTitle(),
                views
        );
    }

    public static EventShortDto toShortDto(Event event, long confirmedRequests, long views) {
        return new EventShortDto(
                event.getId(),
                event.getAnnotation(),
                toCategoryDto(event.getCategory()),
                confirmedRequests,
                event.getEventDate(),
                UserMapper.toShortDto(event.getInitiator()),
                event.getPaid(),
                event.getTitle(),
                views
        );
    }

    private static CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
