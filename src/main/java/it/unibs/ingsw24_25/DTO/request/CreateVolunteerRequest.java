package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVolunteerRequest(
        @NotBlank(message = "Il nickname non può essere vuoto")
        String nickname,
        @NotBlank(message = "La password non può essere vuota")
        @Size(min = 8, message = "La password deve contenere almeno 8 caratteri")
        String password
) {}