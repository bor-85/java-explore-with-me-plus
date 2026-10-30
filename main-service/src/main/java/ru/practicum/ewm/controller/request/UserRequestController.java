package ru.practicum.ewm.controller.request;


import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.service.request.UserRequestService;

import java.util.List;

@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserRequestController {
    private final UserRequestService requestService;

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@RequestParam @PositiveOrZero Long eventId,
                                                 @PathVariable @PositiveOrZero Long userId){
        return requestService.createRequest(userId, eventId);
    }

    @GetMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> findAllUserRequest(@PathVariable @PositiveOrZero Long userId){
        return requestService.findAllUserRequest(userId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelRequest(@PathVariable @PositiveOrZero Long userId,
                                                 @PathVariable @PositiveOrZero Long requestId){
        return requestService.cancelRequest(userId, requestId);
    }
}
