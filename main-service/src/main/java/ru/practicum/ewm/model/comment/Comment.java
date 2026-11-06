package ru.practicum.ewm.model.comment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String text;

    @JoinColumn(name = "event_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    @JoinColumn(name = "author_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;

    @Column(nullable = false)
    private LocalDateTime created;

    private LocalDateTime updated;
}
