package ru.practicum.ewm.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EventRequestStatusUpdateRequest(
        @NotNull
        @Size(min = 1)
        List<Long> requestIds,

        @NotNull
        @Pattern(regexp = "CONFIRMED|REJECTED")
        String status
) {
}