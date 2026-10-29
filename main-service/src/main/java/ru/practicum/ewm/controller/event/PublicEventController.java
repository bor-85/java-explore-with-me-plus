package ru.practicum.ewm.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.StatClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/events")
public class PublicEventController {

    private static final String APP_NAME = "ewm-main-service";

    private final EventService eventService;
    private final StatClient statClient;

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                         @RequestParam(defaultValue = "false") boolean onlyAvailable,
                                         @RequestParam(required = false) @Pattern(regexp = "EVENT_DATE|VIEWS") String sort,
                                         @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                         @RequestParam(defaultValue = "10") @Positive int size,
                                         HttpServletRequest request) {
        saveHit(request);
        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        saveHit(request);
        return eventService.getPublicEvent(id);
    }

    private void saveHit(HttpServletRequest request) {
        statClient.hit(new EndpointHitDto(APP_NAME, request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now()));
    }
}
