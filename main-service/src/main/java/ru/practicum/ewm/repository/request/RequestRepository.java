package ru.practicum.ewm.repository.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.model.request.Request;
import ru.practicum.ewm.model.request.RequestStatus;

import java.util.List;
import java.util.Optional;


public interface RequestRepository extends JpaRepository<Request,Long> {

    boolean existsByEvent_IdAndRequester_Id(long eventId, long userId);

    Optional<Request> findByIdAndRequester_Id(long requestId, long userId);

    long countByEvent_IdAndStatus(long eventId, RequestStatus status);

    List<Request> findAllByRequester_Id(long userId);
}
