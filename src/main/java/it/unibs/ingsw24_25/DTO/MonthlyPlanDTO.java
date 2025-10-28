package it.unibs.ingsw24_25.DTO;

import it.unibs.ingsw24_25.model.PlanningPhase;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class MontthlyPlanDTO {

    private YearMonth targetMonth;
    private PlanningPhase phase;
    private LocalDate availabilityWindowClosedOn;
    private List<PlannedVisitDTO> plannedVisits;
    private List<PlanAvailabilityDTO> availabilitySnapshots;

    public MonthlyPlanDTO(YearMonth targetMonth,
                          PlanningPhase phase,
                          LocalDate availabilityWindowClosedOn,
                          List<PlannedVisitDTO> plannedVisits,
                          List<PlanAvailabilityDTO> availabilitySnapshots) {
        this.targetMonth = Objects.requireNonNull(targetMonth, "targetMonth");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.availabilityWindowClosedOn = availabilityWindowClosedOn;
        this.plannedVisits = plannedVisits;
        this.availabilitySnapshots = availabilitySnapshots;
    }

    public YearMonth getTargetMonth() {
        return targetMonth;
    }

    public PlanningPhase getPhase() {
        return phase;
    }

    public LocalDate getAvailabilityWindowClosedOn() {
        return availabilityWindowClosedOn;
    }

    public List<PlannedVisitDTO> getPlannedVisits() {
        return plannedVisits;
    }

    public List<PlanAvailabilityDTO> getAvailabilitySnapshots() {
        return availabilitySnapshots;
    }
}
