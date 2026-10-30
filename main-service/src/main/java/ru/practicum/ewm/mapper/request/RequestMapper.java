package ru.practicum.ewm.mapper.request;

import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.model.request.Request;

public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(Request request){
        return new ParticipationRequestDto(
                request.getId(),
                request.getEvent().getId(),
                request.getRequester().getId(),
                request.getCreated(),
                request.getStatus().name()
        );
    }
}
