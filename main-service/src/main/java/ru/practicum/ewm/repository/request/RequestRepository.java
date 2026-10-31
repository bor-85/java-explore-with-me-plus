package ru.practicum.ewm.repository.request;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.practicum.ewm.model.request.Request;
import ru.practicum.ewm.model.request.RequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface RequestRepository extends JpaRepository<Request, Long> {

    boolean existsByEvent_IdAndRequester_Id(long eventId, long userId);

    Optional<Request> findByIdAndRequester_Id(long requestId, long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    long countByEvent_IdAndStatus(long eventId, RequestStatus status);

    List<Request> findAllByRequester_Id(long userId);

    List<Request> findAllByEvent_Id(long eventId);

    List<Request> findAllByEvent_IdAndIdIn(long eventId, Collection<Long> requestIds);

    List<Request> findAllByEvent_IdAndStatus(long eventId, RequestStatus status);
}
