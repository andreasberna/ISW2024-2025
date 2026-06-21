package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookVisitRequest(
        @NotBlank(message = "ID visita non può essere vuoto")
        String visitId,
        @Min(value = 1, message = "Il numero di partecipanti deve essere almeno 1")
        int participants,
        String notes
) {}
