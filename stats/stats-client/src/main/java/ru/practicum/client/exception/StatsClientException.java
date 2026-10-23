package ru.practicum.client.exception;

public class StatsClientException extends RuntimeException {
    public StatsClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
