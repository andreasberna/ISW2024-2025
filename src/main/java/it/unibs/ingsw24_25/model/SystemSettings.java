package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class SystemSettings {

    private String territorialScope;
    private int MaxPeoplePerSubscription;
    private List<LocalDate> excludedDates;
    private YearMonth activePlanningMonth;
    private PlanningPhase planningPhase;
    private LocalDate lastAvailabilityWindowClosure;


    public SystemSettings(String territorialScope, int maxPeoplePerSubscription,List<LocalDate> excludedDates) {
        this(territorialScope, maxPeoplePerSubscription, excludedDates, null, PlanningPhase.AVAILABILITY_COLLECTION_OPEN, null);
    }
    public SystemSettings(String territorialScope, int maxPeoplePerSubscription,List<LocalDate> excludedDates,
                          YearMonth activePlanningMonth, PlanningPhase planningPhase,
                          LocalDate lastAvailabilityWindowClosure) {
        this.territorialScope = territorialScope;
        this.MaxPeoplePerSubscription = maxPeoplePerSubscription;
        setExcludedDates(excludedDates);
        this.activePlanningMonth = activePlanningMonth;
        this.planningPhase = planningPhase == null ? PlanningPhase.AVAILABILITY_COLLECTION_OPEN : planningPhase;
        this.lastAvailabilityWindowClosure = lastAvailabilityWindowClosure;
    }

    public String getTerritorialScope() {
        return territorialScope;
    }
    public void setTerritorialScope(String territorialScope) {
        this.territorialScope = territorialScope;
    }
    public int getMaxPeoplePerSubscription() {
        return MaxPeoplePerSubscription;
    }
    public void setMaxPeoplePerSubscription(int maxPeoplePerSubscription) {
        this.MaxPeoplePerSubscription = maxPeoplePerSubscription;
    }
    public List<LocalDate> getExcludedDates() {
        return excludedDates == null ? List.of() : Collections.unmodifiableList(excludedDates);
    }
    public void setExcludedDates(List<LocalDate> excludedDates) {
        if (excludedDates == null) {this.excludedDates = new ArrayList<>();}
        else this.excludedDates = new ArrayList<> (excludedDates);
    }
    public YearMonth getActivePlanningMonth() {
        return activePlanningMonth;
    }

    public void setActivePlanningMonth(YearMonth activePlanningMonth) {
        this.activePlanningMonth = activePlanningMonth;
    }

    public PlanningPhase getPlanningPhase() {
        if (planningPhase == null) {
            planningPhase = PlanningPhase.AVAILABILITY_COLLECTION_OPEN;
        }
        return planningPhase;
    }

    public void setPlanningPhase(PlanningPhase planningPhase) {
        if (planningPhase == null) {
            throw new IllegalArgumentException("La fase di pianificazione non può essere nulla");
        }
        this.planningPhase = planningPhase;
    }

    public LocalDate getLastAvailabilityWindowClosure() {
        return lastAvailabilityWindowClosure;
    }

    public void setLastAvailabilityWindowClosure(LocalDate lastAvailabilityWindowClosure) {
        this.lastAvailabilityWindowClosure = lastAvailabilityWindowClosure;
    }


}
