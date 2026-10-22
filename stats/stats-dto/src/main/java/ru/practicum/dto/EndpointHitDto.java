package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EndpointHitDto(
        Long id,
        @NotBlank String app,
        @NotBlank String uri,
        @NotBlank String ip,
        @NotNull @JsonFormat(pattern = EndpointHitDto.DATE_TIME_PATTERN) LocalDateTime timestamp
) {
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
}
