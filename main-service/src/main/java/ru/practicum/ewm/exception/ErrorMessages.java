package ru.practicum.ewm.exception;

public final class ErrorMessages {
    public static final String USER_NOT_FOUND = "User with id=%d was not found";
    public static final String CATEGORY_NOT_FOUND = "Category with id=%d was not found";
    public static final String CATEGORY_ALREADY_EXISTS = "Category with name '%s' already exists";
    public static final String CATEGORY_IS_NOT_EMPTY = "The category is not empty";
    public static final String EVENT_NOT_FOUND = "Event with id=%d was not found";
    public static final String EVENT_NOT_EDITABLE = "Only pending or canceled events can be changed";
    public static final String EVENT_DATE_TOO_EARLY = "Event date must be at least %d hours from now: %s";
    public static final String EVENT_NOT_PENDING = "Cannot publish the event because it is not in the right state: %s";
    public static final String EVENT_ALREADY_PUBLISHED = "Cannot reject the event because it is already published";
    public static final String EVENT_PUBLISH_TOO_LATE = "Event date must be at least %d hours after publication: %s";
    public static final String EVENT_OUT_OF_LIMIT = "The participant limit for this event has been reached";
    public static final String EVENT_STATUS_IS_NOT_PUBLISHED = "Event status is not PUBLISHED";
    public static final String RANGE_START_AFTER_END = "RangeStart must not be after rangeEnd";
    public static final String SELF_REGISTRATION_FORBIDDEN = "You cannot join an event you created";
    public static final String REQUEST_NOT_FOUND = "Request with id=%d not found for user with id=%d";
    public static final String REQUEST_ALREADY_EXISTS = "User with id=%d has already been sent request for event with id=%d";


    public static final String COMPILATION_NOT_FOUND = "Compilation with id=%d was not found";
    public static final String COMPILATION_ALREADY_EXISTS = "Compilation with title '%s' already exists";

    private ErrorMessages() {
    }
}