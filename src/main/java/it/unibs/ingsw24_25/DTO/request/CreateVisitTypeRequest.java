package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record CreateVisitTypeRequest(
        @NotBlank(message = "ID del luogo non può essere vuoto")
        String placeId,
        @NotBlank(message = "Il titolo della visita non può essere vuoto")
        String title,
        @NotBlank(message = "La descrizione non può essere vuota")
        String description,
        @NotBlank(message = "Il punto d'incontro non può essere vuoto")
        String meetingPoint,
        @NotNull(message = "La lista degli orari non può essere nulla")
        @NotEmpty(message = "È necessario specificare almeno un orario")
        @Valid
        List<ScheduleRequest> schedules,
        boolean ticketRequired,
        @Min(value = 1, message = "Il numero minimo di partecipanti deve essere almeno 1")
        int minParticipants,
        @Min(value = 1, message = "Il numero massimo di partecipanti deve essere almeno 1")
        int maxParticipants,
        LocalDate startDate,
        LocalDate endDate
) {
    @AssertTrue(message = "minParticipants deve essere <= maxParticipants")
    public boolean isValidParticipantRange() {
        return minParticipants <= maxParticipants;
    }

    @AssertTrue(message = "La data di fine deve essere successiva o uguale alla data di inizio")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) return true;
        return !endDate.isBefore(startDate);
    }
}
