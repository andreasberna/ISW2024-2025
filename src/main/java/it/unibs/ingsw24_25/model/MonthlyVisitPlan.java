package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Entity
@Table(name = "monthly_visit_plans")
public class MonthlyVisitPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "`version`")
    private Long version;

    @Column(nullable = false, unique = true)
    private YearMonth targetMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanningPhase phase = PlanningPhase.RACCOLTA_DISPONIBILITA;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "monthly_plan_excluded_dates", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "excluded_date")
    private Set<LocalDate> excludedDates = new HashSet<>();

    @OneToMany(mappedBy = "monthlyPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlannedVisit> visits = new ArrayList<>();

    protected MonthlyVisitPlan() {}

    public MonthlyVisitPlan(YearMonth targetMonth) {
        this.targetMonth = targetMonth;
    }

    public Long getId() {
        return id;
    }

    public YearMonth getTargetMonth() {
        return targetMonth;
    }

    public PlanningPhase getPhase() {
        return phase;
    }

    public void setPhase(PlanningPhase phase) {
        if (phase == null)
            throw new IllegalArgumentException("La fase di pianificazione non può essere nulla");
        if (!this.phase.canTransitionTo(phase))
            throw new IllegalStateException(String.format("Transizione di fase %s -> %s non è valida", this.phase, phase));
        this.phase = phase;
    }

    public Set<LocalDate> getExcludedDates() {
        return Collections.unmodifiableSet(excludedDates);
    }

    public void addExcludedDate(LocalDate date) {
        Objects.requireNonNull(date, "La data esclusa non può essere nulla");
        this.excludedDates.add(date);
    }

    public List<PlannedVisit> getVisits() {
        return Collections.unmodifiableList(visits);
    }

    public void setVisits(List<PlannedVisit> visits) {
        this.visits.clear();
        if (visits != null) {
            this.visits.addAll(visits);
        }
    }
}
