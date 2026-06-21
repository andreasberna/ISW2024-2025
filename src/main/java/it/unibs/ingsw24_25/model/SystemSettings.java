package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    private Long id;

    @Version
    @Column(name = "`version`")
    private Long version;

    private String territorialScope;
    private int maxPeoplePerSubscription;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "excluded_dates", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "excluded_date")
    private List<LocalDate> excludedDates;

    private YearMonth activePlanningMonth;

    @Enumerated(EnumType.STRING)
    private PlanningPhase planningPhase;
    
    private LocalDate lastAvailabilityWindowClosure;

    protected SystemSettings() {}

    public SystemSettings(String territorialScope, int maxPeoplePerSubscription, List<LocalDate> excludedDates) {
        this(1L, territorialScope, maxPeoplePerSubscription, excludedDates, null, PlanningPhase.RACCOLTA_DISPONIBILITA, null);
    }

    public SystemSettings(Long id, String territorialScope, int maxPeoplePerSubscription, List<LocalDate> excludedDates,
                          YearMonth activePlanningMonth, PlanningPhase planningPhase,
                          LocalDate lastAvailabilityWindowClosure) {
        this.id = id;
        this.territorialScope = territorialScope;
        this.maxPeoplePerSubscription = maxPeoplePerSubscription;
        setExcludedDates(excludedDates);
        this.activePlanningMonth = activePlanningMonth;
        this.planningPhase = planningPhase == null ? PlanningPhase.RACCOLTA_DISPONIBILITA : planningPhase;
        this.lastAvailabilityWindowClosure = lastAvailabilityWindowClosure;
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getTerritorialScope() {
        return territorialScope;
    }

    public void setTerritorialScope(String territorialScope) {
        this.territorialScope = territorialScope;
    }

    public int getMaxPeoplePerSubscription() {
        return maxPeoplePerSubscription;
    }

    public void setMaxPeoplePerSubscription(int maxPeoplePerSubscription) {
        this.maxPeoplePerSubscription = maxPeoplePerSubscription;
    }

    public List<LocalDate> getExcludedDates() {
        return excludedDates == null ? List.of() : Collections.unmodifiableList(excludedDates);
    }

    public void setExcludedDates(List<LocalDate> excludedDates) {
        if (excludedDates == null) {
            this.excludedDates = new ArrayList<>();
        } else {
            this.excludedDates = new ArrayList<>(excludedDates);
        }
    }

    public YearMonth getActivePlanningMonth() {
        return activePlanningMonth;
    }

    public void setActivePlanningMonth(YearMonth activePlanningMonth) {
        this.activePlanningMonth = activePlanningMonth;
    }

    public PlanningPhase getPlanningPhase() {
        if (planningPhase == null) {
            planningPhase = PlanningPhase.RACCOLTA_DISPONIBILITA;
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
