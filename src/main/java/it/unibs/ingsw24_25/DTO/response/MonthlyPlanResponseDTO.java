package it.unibs.ingsw24_25.DTO.response;

import it.unibs.ingsw24_25.model.PlanningPhase;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public record MonthlyPlanResponseDTO(Long id, YearMonth targetMonth, PlanningPhase phase) {
    public String getFormattedTargetMonth() {
        if (targetMonth == null) {
            return "";
        }
        return targetMonth.format(DateTimeFormatter.ofPattern("MM/yyyy"));
    }
}