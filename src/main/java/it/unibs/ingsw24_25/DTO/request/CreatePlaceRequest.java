package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePlaceRequest(
        @NotBlank(message = "Il titolo del luogo non può essere vuoto")
        String title,
        @NotBlank(message = "La località non può essere vuota")
        String location
) {}