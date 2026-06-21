package it.unibs.ingsw24_25.DTO.response;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.Set;

public record VolunteerAvailabilityResponse(
        YearMonth referenceMonth,
        Set<DayOfWeek> preferredDays,
        int weeklyFrequency
) {}
