package it.unibs.ingsw24_25.DTO.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRequest(
    @NotNull(message = "Il giorno della settimana non può essere nullo")
    DayOfWeek dayOfWeek,
    @NotNull(message = "L'orario di inizio non può essere nullo")
    LocalTime startTime,
    @Min(value = 15, message = "La durata minima è 15 minuti")
    @Max(value = 480, message = "La durata massima è 480 minuti (8 ore)")
    int durationInMinutes
) {}
