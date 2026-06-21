package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.YearMonth;
import java.time.LocalDate;

@Entity
@Table(name = "assigned_shifts")
public class AssignedShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private Volunteer volunteer;

    @Column(name = "`month`", nullable = false)
    private YearMonth month;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String visitTypeId;

    @Embedded
    private TimeSlot slot;

    protected AssignedShift() {}

    public AssignedShift(Volunteer volunteer, YearMonth month, LocalDate date, String visitTypeId, TimeSlot slot) {
        this.volunteer = volunteer;
        this.month = month;
        this.date = date;
        this.visitTypeId = visitTypeId;
        this.slot = slot;
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

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getVisitTypeId() {
        return visitTypeId;
    }

    public void setVisitTypeId(String visitTypeId) {
        this.visitTypeId = visitTypeId;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public void setSlot(TimeSlot slot) {
        this.slot = slot;
    }
}
