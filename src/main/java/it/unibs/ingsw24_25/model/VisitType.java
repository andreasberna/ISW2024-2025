package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "visit_types")
public class VisitType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String visitTitle;

    @Lob
    private String description;

    private String meetLocation;

    private LocalDate validFrom;
    private LocalDate validTo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "visit_type_schedules", joinColumns = @JoinColumn(name = "visit_type_id"))
    private List<TimeSlot> schedules = new ArrayList<>();

    private boolean ticketRequired;
    private int minParticipants;
    private int maxParticipants;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @OneToMany(mappedBy = "visitType", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PlannedVisit> plannedVisits = new ArrayList<>();

    protected VisitType() {}

    public VisitType(String visitTitle, String description, String meetLocation, LocalDate validFrom, LocalDate validTo,
                     List<TimeSlot> schedules, boolean ticketRequired, int minParticipants, int maxParticipants, Place place) {
        this.id = UUID.randomUUID().toString();
        this.visitTitle = visitTitle;
        this.description = description;
        this.meetLocation = meetLocation;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.schedules = schedules;
        this.ticketRequired = ticketRequired;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.place = place;
    }
    
    public String getId() {
        return id;
    }

    public String getVisitTitle() {
        return visitTitle;
    }

    public String getDescription() {
        return description;
    }

    public String getMeetLocation() {
        return meetLocation;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public List<TimeSlot> getSchedules() {
        return schedules;
    }

    public boolean isTicketRequired() {
        return ticketRequired;
    }

    public int getMinParticipants() {
        return minParticipants;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public Place getPlace() {
        return place;
    }

    @Transient
    public int getAvailableSlots(PlannedVisit plannedVisit) {
        if (plannedVisit == null) return maxParticipants;
        return maxParticipants - plannedVisit.countBookedParticipants();
    }

    @Transient
    public boolean isFull(PlannedVisit plannedVisit) {
        return getAvailableSlots(plannedVisit) <= 0;
    }

    @Transient
    public boolean meetsMinimumParticipants(PlannedVisit plannedVisit) {
        if (plannedVisit == null) return false;
        return plannedVisit.countBookedParticipants() >= minParticipants;
    }

    public List<PlannedVisit> generateOccurrences(YearMonth month) {
        List<PlannedVisit> occurrences = new ArrayList<>();
        if (month == null) {
            return occurrences;
        }

        for (LocalDate date = month.atDay(1); date.isBefore(month.atEndOfMonth().plusDays(1)); date = date.plusDays(1)) {
            if ((validFrom != null && date.isBefore(validFrom)) || (validTo != null && date.isAfter(validTo))) {
                continue;
            }
            for (TimeSlot slot : schedules) {
                if (slot.getDay() == date.getDayOfWeek()) {
                    occurrences.add(new PlannedVisit(null, this, null, date, slot.getStartTime()));
                }
            }
        }
        return occurrences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisitType visitType = (VisitType) o;
        return Objects.equals(id, visitType.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
