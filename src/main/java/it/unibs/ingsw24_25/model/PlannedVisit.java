package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "planned_visits")
public class PlannedVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_plan_id", nullable = false)
    private MonthlyVisitPlan monthlyPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_type_id", nullable = false)
    private VisitType visitType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id")
    private Volunteer volunteer;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false)
    private LocalTime visitTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "planned_visit_bookings", joinColumns = @JoinColumn(name = "planned_visit_id"))
    private List<VisitBooking> bookings = new ArrayList<>();

    protected PlannedVisit() {}

    public PlannedVisit(MonthlyVisitPlan monthlyPlan, VisitType visitType, Volunteer volunteer, LocalDate visitDate, LocalTime visitTime) {
        this.monthlyPlan = monthlyPlan;
        this.visitType = visitType;
        this.volunteer = volunteer;
        this.visitDate = visitDate;
        this.visitTime = visitTime;
        this.status = VisitStatus.PROPOSTA;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public MonthlyVisitPlan getMonthlyPlan() {
        return monthlyPlan;
    }

    public void setMonthlyPlan(MonthlyVisitPlan monthlyPlan) {
        this.monthlyPlan = monthlyPlan;
    }

    public VisitType getVisitType() {
        return visitType;
    }

    public void setVisitType(VisitType visitType) {
        this.visitType = visitType;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public LocalTime getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(LocalTime visitTime) {
        this.visitTime = visitTime;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        this.status = status;
    }

    public List<VisitBooking> getBookings() {
        return Collections.unmodifiableList(bookings);
    }

    public void addBooking(VisitBooking booking) {
        this.bookings.add(booking);
    }

    public void removeBooking(String code) {
        this.bookings.removeIf(b -> b.getCode().equals(code));
    }

    /** Somma i partecipanti di tutte le prenotazioni attive per questa visita. */
    public int countBookedParticipants() {
        return bookings.stream().mapToInt(VisitBooking::getParticipants).sum();
    }
}
