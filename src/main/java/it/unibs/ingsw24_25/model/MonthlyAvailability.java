package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "monthly_availabilities")
public class MonthlyAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id")
    private Volunteer volunteer;

    @Column(nullable = false)
    private YearMonth referenceMonth;

    @ElementCollection(targetClass = DayOfWeek.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "preferred_days", joinColumns = @JoinColumn(name = "availability_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private Set<DayOfWeek> preferredDays = EnumSet.noneOf(DayOfWeek.class);

    @Column(nullable = false)
    private int weeklyFrequency;

    private LocalDate submittedOn;

    private boolean isSnapshot = false;

    private LocalDate snapshotCapturedOn;

    protected MonthlyAvailability() {}

    public MonthlyAvailability(YearMonth referenceMonth, Set<DayOfWeek> preferredDays, int weeklyFrequency, LocalDate submittedOn, boolean isSnapshot, LocalDate snapshotCapturedOn) {
        this.referenceMonth = referenceMonth;
        this.preferredDays = preferredDays;
        this.weeklyFrequency = weeklyFrequency;
        this.submittedOn = submittedOn;
        this.isSnapshot = isSnapshot;
        this.snapshotCapturedOn = snapshotCapturedOn;
    }
    
    public void ensureConsistency() {
        // Method used in MonthlyVisitPlan, can be left empty or implemented
    }


    public Long getId() {
        return id;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public YearMonth getReferenceMonth() {
        return referenceMonth;
    }

    public YearMonth getMonth() {
        return referenceMonth;
    }

    public Set<DayOfWeek> getPreferredDays() {
        return preferredDays;
    }

    public int getWeeklyFrequency() {
        return weeklyFrequency;
    }

    public int getPreferredOccurrences() {
        return weeklyFrequency;
    }

    public LocalDate getSubmittedOn() {
        return submittedOn;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    public LocalDate getSnapshotCapturedOn() {
        return snapshotCapturedOn;
    }
}