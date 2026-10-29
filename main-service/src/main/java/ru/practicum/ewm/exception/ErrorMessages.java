package ru.practicum.ewm.exception;

public final class ErrorMessages {
    public static final String USER_NOT_FOUND = "Пользователь с id=%d не найден";
    public static final String CATEGORY_NOT_FOUND = "Категория с id=%d не найдена";
    public static final String EVENT_NOT_FOUND = "Событие с id=%d не найдено";
    public static final String EVENT_NOT_EDITABLE = "Изменить можно только отменённые события или события в ожидании модерации";
    public static final String EVENT_DATE_TOO_EARLY = "Дата события должна быть не раньше чем через %d ч. от текущего момента: %s";
    public static final String EVENT_NOT_PENDING = "Опубликовать можно только событие в ожидании модерации, текущее состояние: %s";
    public static final String EVENT_ALREADY_PUBLISHED = "Нельзя отклонить уже опубликованное событие";
    public static final String EVENT_PUBLISH_TOO_LATE = "Дата события должна быть не раньше чем через %d ч. от даты публикации: %s";
    public static final String RANGE_START_AFTER_END = "Дата начала диапазона не может быть позже даты окончания";

    private ErrorMessages() {
    }
}
