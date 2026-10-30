package ru.practicum.ewm.model.request;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "event_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    @JoinColumn(name = "requester_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User requester;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;
}
