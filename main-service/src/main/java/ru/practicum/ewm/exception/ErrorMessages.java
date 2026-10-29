package ru.practicum.ewm.exception;

public final class ErrorMessages {
    public static final String USER_NOT_FOUND = "User with id=%d was not found";
    public static final String CATEGORY_NOT_FOUND = "Category with id=%d was not found";
    public static final String EVENT_NOT_FOUND = "Event with id=%d was not found";
    public static final String EVENT_NOT_EDITABLE = "Only pending or canceled events can be changed";
    public static final String EVENT_DATE_TOO_EARLY = "Event date must be at least %d hours from now: %s";
    public static final String EVENT_NOT_PENDING = "Cannot publish the event because it is not in the right state: %s";
    public static final String EVENT_ALREADY_PUBLISHED = "Cannot reject the event because it is already published";
    public static final String EVENT_PUBLISH_TOO_LATE = "Event date must be at least %d hours after publication: %s";
    public static final String RANGE_START_AFTER_END = "rangeStart must not be after rangeEnd";

    private ErrorMessages() {
    }
}
