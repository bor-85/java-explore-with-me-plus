package ru.practicum.ewm.mapper.comment;

import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.mapper.user.UserMapper;
import ru.practicum.ewm.model.comment.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentDto toDto(Comment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                comment.getEvent().getId(),
                UserMapper.toShortDto(comment.getAuthor()),
                comment.getCreated(),
                comment.getUpdated()
        );
    }
}
