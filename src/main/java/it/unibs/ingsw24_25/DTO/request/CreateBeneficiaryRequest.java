package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBeneficiaryRequest(
        @NotBlank(message = "Il nome completo non può essere vuoto")
        String fullName,
        @NotBlank(message = "Lo username non può essere vuoto")
        String username,
        @NotBlank(message = "La password non può essere vuota")
        String password
) {}
