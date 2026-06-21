package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BeneficiaryRegistrationRequest(
        @NotBlank(message = "Il nome completo non può essere vuoto")
        String fullName,
        @NotBlank(message = "L'username non può essere vuoto")
        String username,
        @NotBlank(message = "La password non può essere vuota")
        @Size(min = 8, message = "La password deve contenere almeno 8 caratteri")
        String password
) {}
