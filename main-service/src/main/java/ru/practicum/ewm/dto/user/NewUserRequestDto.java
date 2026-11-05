package ru.practicum.ewm.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewUserRequestDto(
        @NotBlank
        @Size(min = 2, max = 250)
        String name,

        @NotBlank
        @Email
        @Size(min = 6, max = 254)
        String email
) {
}