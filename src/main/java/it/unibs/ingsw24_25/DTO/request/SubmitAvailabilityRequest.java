package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.Set;

public record SubmitAvailabilityRequest(
        @NotNull(message = "Il mese non può essere nullo")
        @FutureOrPresent(message = "Il mese deve essere presente o futuro")
        YearMonth month,
        @NotEmpty(message = "I giorni disponibili non possono essere vuoti")
        Set<DayOfWeek> availableDays
) {}