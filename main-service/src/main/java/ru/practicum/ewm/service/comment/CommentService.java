package ru.practicum.ewm.service.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ErrorMessages;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.comment.CommentMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.comment.Comment;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.repository.comment.CommentRepository;
import ru.practicum.ewm.repository.event.EventRepository;
import ru.practicum.ewm.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId)));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.EVENT_NOT_FOUND.formatted(eventId)));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException(ErrorMessages.EVENT_STATUS_IS_NOT_PUBLISHED);
        }

        Comment comment = new Comment();
        comment.setText(dto.text());
        comment.setEvent(event);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        Comment comment = getAuthorComment(userId, commentId);
        comment.setText(dto.text());
        comment.setUpdated(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return CommentMapper.toDto(comment);
    }

    @Transactional
    public void deleteUserComment(Long userId, Long commentId) {
        commentRepository.delete(getAuthorComment(userId, commentId));
    }

    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        checkUserExists(userId);
        return commentRepository.findAuthorPage(userId, from, size).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        if (!eventRepository.existsByIdAndState(eventId, EventState.PUBLISHED)) {
            throw new NotFoundException(ErrorMessages.EVENT_NOT_FOUND.formatted(eventId));
        }
        return commentRepository.findEventPage(eventId, from, size).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    public CommentDto getComment(Long commentId) {
        return commentRepository.findByIdAndEvent_State(commentId, EventState.PUBLISHED)
                .map(CommentMapper::toDto)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.COMMENT_NOT_FOUND.formatted(commentId)));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.COMMENT_NOT_FOUND.formatted(commentId)));
        commentRepository.delete(comment);
    }

    private Comment getAuthorComment(Long userId, Long commentId) {
        checkUserExists(userId);
        return commentRepository.findByIdAndAuthor_Id(commentId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessages.COMMENT_NOT_FOUND.formatted(commentId)));
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(ErrorMessages.USER_NOT_FOUND.formatted(userId));
        }
    }
}
