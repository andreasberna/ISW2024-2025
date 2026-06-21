package it.unibs.ingsw24_25.DTO;

import java.util.List;
import java.time.LocalDate;

public record SystemSettingsDTO(String territorialScope, int maxPeoplePerSubscription, List<LocalDate> excludedDates) {
}