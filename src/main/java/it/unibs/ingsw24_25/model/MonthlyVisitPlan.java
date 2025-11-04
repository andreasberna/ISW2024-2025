package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class MonthlyVisitPlan {

    private YearMonth targetMonth;
    private PlanningPhase phase = PlanningPhase.AVAILABILITY_COLLECTION_OPEN;
    private LocalDate availabilityWindowClosedOn;
    private List<PlannedVisit> plannedVisits = new ArrayList<>();
    private Map<String, MonthlyAvailability> availabilitySnapshots = new HashMap<> ();


    public MonthlyVisitPlan(){
        //costruttore di default richiesto per la (de)serializzazione JSON
    }
    public MonthlyVisitPlan(YearMonth targetMonth){
        this(targetMonth, PlanningPhase.AVAILABILITY_COLLECTION_OPEN, null, List.of(), Map.of ());
    }

    public MonthlyVisitPlan (YearMonth yearMonth, PlanningPhase phase,
                             LocalDate availabilityWindowClosedOn, List<PlannedVisit> plannedVisits,
                             Map<String, MonthlyAvailability> availabilitySnapshot) {
        setTargetMonth(yearMonth);
        this.phase = phase == null ? PlanningPhase.AVAILABILITY_COLLECTION_OPEN : phase;
        this.availabilityWindowClosedOn = availabilityWindowClosedOn;
        setPlannedVisits(plannedVisits);
        setAvailabilitySnapshots(availabilitySnapshot);
    }

    public YearMonth getTargetMonth() {
        return targetMonth;
    }
    public void setTargetMonth(YearMonth targetMonth) {
        this.targetMonth = Objects.requireNonNull (targetMonth);
    }
    public PlanningPhase getPhase() {
        return phase;
    }
    public void setPhase(PlanningPhase phase) {
        if (phase == null)
            throw new IllegalArgumentException ("La fase di pianificazione non può essere nulla");
        if (!this.phase.canTransitionTo(phase))
            throw new IllegalStateException (String.format ("Transizione di fase %s -> %s non è valida", this.phase, phase));

        this.phase = phase;
    }
    public LocalDate getAvailabilityWindowClosedOn() {
        return availabilityWindowClosedOn;
    }
    public void setAvailabilityWindowClosedOn(LocalDate availabilityWindowClosedOn) {
        this.availabilityWindowClosedOn = availabilityWindowClosedOn;
    }

    public List<PlannedVisit> getPlannedVisits() {
        return Collections.unmodifiableList(plannedVisits);
    }
    public void setPlannedVisits(List<PlannedVisit> plannedVisits) {
        this.plannedVisits = new ArrayList<>();
        if (plannedVisits == null) {return;}

        plannedVisits.stream()
                .filter(Objects::nonNull)
                .map(this::copyVisit)
                .forEach (this.plannedVisits::add);
    }
    public Map<String, MonthlyAvailability> getAvailabilitySnapshots() {
        return Collections.unmodifiableMap(availabilitySnapshots);
    }
    public void setAvailabilitySnapshots(Map<String, MonthlyAvailability> availabilitySnapshots) {
        this.availabilitySnapshots = new HashMap<> ();
        if (availabilitySnapshots == null) return;
        availabilitySnapshots.forEach ((nickname, availability) -> {
            if (nickname != null && availability != null)
                this.availabilitySnapshots.put(nickname, copyAvailability(availability));
        });
    }

    public void recordAvailabilitySnapshot(String volunteerId, MonthlyAvailability availability) {
        Objects.requireNonNull(volunteerId, "L'identificativo del volontario non può essere nullo");
        Objects.requireNonNull(availability, "La disponibilità non può essere nulla");
        availability.ensureConsistency();
        this.availabilitySnapshots.put(volunteerId, copyAvailability(availability));
    }

    public void removeAvailabilitySnapshot(String volunteerId) {
        if (volunteerId == null) {
            return;
        }
        availabilitySnapshots.remove(volunteerId);
    }

    public void addPlannedVisit(PlannedVisit plannedVisit) {
        Objects.requireNonNull(plannedVisit, "La visita pianificata non può essere nulla");
        this.plannedVisits.add(copyVisit(plannedVisit));
    }

    public void removePlannedVisit(PlannedVisit plannedVisit) {
       if (plannedVisit == null) {return;}
       this.plannedVisits.removeIf (visit -> visit != null && Objects.equals(visit.getId(), plannedVisit.getId()));
    }

    public void replacePlannedVisits(List<PlannedVisit> newPlannedVisits) {
        this.plannedVisits.clear();
        if (newPlannedVisits != null) {
            newPlannedVisits.stream()
                    .filter(Objects::nonNull)
                    .map (this::copyVisit)
                    .forEach (this.plannedVisits::add);
        }
    }

    public boolean removeVisitsByVisitType(String visitTypeId) {
        Objects.requireNonNull(visitTypeId, "L'identificativo del tipo visita non può essere nullo");
        return plannedVisits.removeIf(visit -> visitTypeId.equals(visit.getVisitTypeId()));
    }

    public boolean removeVisitsByVisitTypes(Iterable<String> visitTypeIds) {
        Objects.requireNonNull(visitTypeIds, "La collezione di identificativi non può essere nulla");
        boolean modified = false;
        for (String id : visitTypeIds) {
            if (id != null) {
                modified |= removeVisitsByVisitType(id);
            }
        }
        return modified;
    }

    public boolean removeVolunteerAssignments(String volunteerId) {
        Objects.requireNonNull(volunteerId, "L'identificativo del volontario non può essere nullo");
        boolean modified = false;
        for (PlannedVisit visit : plannedVisits) {
            if (visit != null) {
                modified |= visit.unassignVolunteer(volunteerId);
            }
        }
        modified |= availabilitySnapshots.remove(volunteerId) != null;
        return modified;
    }

    private MonthlyAvailability copyAvailability(MonthlyAvailability availability) {
        availability.ensureConsistency();
        return new MonthlyAvailability(
                availability.getReferenceMonth(),
                EnumSet.copyOf(availability.getPreferredDays()),
                availability.getWeeklyFrequency(),
                availability.getSubmittedOn(),
                availability.isSnapshot(),
                availability.getSnapshotCapturedOn()
        );
    }

    public Optional<PlannedVisit> findVisitById(String visitId) {
        if (visitId == null || visitId.isBlank ()) return Optional.empty();

        return plannedVisits.stream()
                .filter (Objects::nonNull)
                .filter (visit -> visitId.equals (visit.getId()))
                .findFirst ();
    }

    private PlannedVisit copyVisit(PlannedVisit visit) {
        if (visit == null) return null;

        TimeSlot slot = visit.getTimeSlot();
        TimeSlot clonedSlot = slot == null ? null : new TimeSlot (slot.getDay (), slot.getStartTime (), slot.getDuration ());

        return new PlannedVisit (
                visit.getId(),
                visit.getDate (),
                clonedSlot,
                visit.getVisitTypeId (),
                visit.isProposable (),
                visit.getAssignedVolunteerIds (),
                visit.getStatus(),
                visit.getBookings()
        );
    }
}
