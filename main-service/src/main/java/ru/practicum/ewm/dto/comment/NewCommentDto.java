package ru.practicum.ewm.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewCommentDto(
        @NotBlank
        @Size(max = 2000)
        String text
) {
}
