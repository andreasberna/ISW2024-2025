package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Il nickname non può essere vuoto")
        String nickname,
        @NotBlank(message = "La password non può essere vuota")
        String password
) {}