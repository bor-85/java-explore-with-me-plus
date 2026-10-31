package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ErrorMessages;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.request.RequestMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.request.Request;
import ru.practicum.ewm.model.request.RequestStatus;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.request.RequestRepository;
import ru.practicum.ewm.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRequestService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;


    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId));
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.EVENT_NOT_FOUND.formatted(eventId)));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(ErrorMessages.SELF_REGISTRATION_FORBIDDEN);
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException(ErrorMessages.EVENT_STATUS_IS_NOT_PUBLISHED);
        }

        if (requestRepository.existsByEvent_IdAndRequester_Id(eventId, userId)) {
            throw new ConflictException(ErrorMessages.REQUEST_ALREADY_EXISTS.formatted(userId, eventId));
        }

        User requesterProxy = userRepository.getReferenceById(userId);

        Request request = new Request();
        request.setCreated(LocalDateTime.now());
        request.setRequester(requesterProxy);
        request.setEvent(event);
        long confirmedRequests = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException(ErrorMessages.EVENT_OUT_OF_LIMIT);
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    public List<ParticipationRequestDto> findAllUserRequest(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId));
        }

        return requestRepository.findAllByRequester_Id(userId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId));
        }
        Request request = requestRepository.findByIdAndRequester_Id(requestId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.REQUEST_NOT_FOUND.formatted(requestId, userId)));

        request.setStatus(RequestStatus.CANCELED);

        return RequestMapper.toParticipationRequestDto(request);
    }
}
