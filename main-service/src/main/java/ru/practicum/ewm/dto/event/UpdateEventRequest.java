package ru.practicum.ewm.dto.event;

import java.time.LocalDateTime;

// Common fields of user and admin update requests, implemented by records automatically
public interface UpdateEventRequest {
    String annotation();

    Long category();

    String description();

    LocalDateTime eventDate();

    Location location();

    Boolean paid();

    Integer participantLimit();

    Boolean requestModeration();

    String stateAction();

    String title();
}
