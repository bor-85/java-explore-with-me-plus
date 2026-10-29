package ru.practicum.ewm.exception;

public final class ErrorMessages {
    public static final String USER_NOT_FOUND = "Пользователь с id=%d не найден";
    public static final String CATEGORY_NOT_FOUND = "Категория с id=%d не найдена";
    public static final String EVENT_NOT_FOUND = "Событие с id=%d не найдено";
    public static final String EVENT_NOT_EDITABLE = "Изменить можно только отменённые события или события в ожидании модерации";
    public static final String EVENT_DATE_TOO_EARLY = "Дата события должна быть не раньше чем через %d ч. от текущего момента: %s";

    private ErrorMessages() {
    }
}
