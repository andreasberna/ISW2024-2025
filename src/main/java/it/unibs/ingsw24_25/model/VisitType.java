package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class VisitType {
    private String id;
    private String visitTitle;
    private String visitDescription;
    private String visitMeetLocation;
    private LocalDate validFrom;
    private LocalDate validTo;
    private List<TimeSlot> schedules;
    private Boolean ticketRequired;
    private int minParticipants;
    private int maxParticipants;
    private Place place;
    private List<Volunteer> guides;
    private VisitState state;
    private LocalDate visitDate;
    private LocalDate enrollmentDeadline;
    private int enrolled;

    public VisitType(String visitTitle, String visitDescription, String visitMeetLocation,
                     LocalDate validFrom, LocalDate validTo, List<TimeSlot> schedules,
                     Boolean ticketRequired, int minParticipants, int maxParticipants,
                     Place place, List<Volunteer> guides ) {
        this(UUID.randomUUID().toString(), visitTitle, visitDescription, visitMeetLocation,
                validFrom, validTo, schedules, ticketRequired, minParticipants, maxParticipants,
                place, guides);
    }

    public VisitType(String id, String visitTitle, String visitDescription, String visitMeetLocation,
                     LocalDate validFrom, LocalDate validTo, List<TimeSlot> schedules,
                     Boolean ticketRequired, int minParticipants, int maxParticipants,
                     Place place, List<Volunteer> guides ) {
        this.id = sanitizeId(id);
        this.visitTitle = visitTitle;
        this.visitDescription = visitDescription;
        this.visitMeetLocation = visitMeetLocation;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.schedules = schedules == null ? new ArrayList<> () : new ArrayList<>(schedules);
        this.ticketRequired = ticketRequired;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.place = place;
        this.guides = guides == null ? new ArrayList<> () : new ArrayList<>(guides);
    }
    public VisitType(){
        // costruttore per la (de)serializzazione
    }

    public void addSchedule(TimeSlot timeSlot){
        ensureSchedulesInitialized();
        this.schedules.add(timeSlot);
    }
    public void removeSchedule(TimeSlot timeSlot){
        ensureSchedulesInitialized();
        this.schedules.remove(timeSlot);
    }
    public void addGuide(Volunteer volunteer){
        ensureGuidesInitialized();
        this.guides.add(volunteer);
    }
    public void removeGuide(Volunteer volunteer){
        ensureGuidesInitialized();
        this.guides.remove(volunteer);
    }

    public void addEnrollment(int n){
        if(state == VisitState.CANCELLATA || state == VisitState.CONFERMATA)
            throw new IllegalStateException("Iscrizione non consentita in stato: " + state);

        if(enrolled + n > maxParticipants)
            throw new IllegalStateException("Superato il numero massimo di partecipanti");

        enrolled +=n;
        updateState(LocalDate.now ());
    }
    public void removeEnrollment(int n){
        if (enrolled - n < 0)
            throw new IllegalStateException("Numero di cancellazioni non valido");

        enrolled -=n;
        updateState(LocalDate.now());
    }

    public void updateState(LocalDate today){
        if (visitDate == null){return;}

        LocalDate deadline = visitDate.minusDays (3);

        switch (state) {
            case PROPOSTA -> {
                if(enrolled == maxParticipants) state = VisitState.COMPLETA;
                else if(today.isEqual (deadline)){
                    if (enrolled >= minParticipants) state = VisitState.CONFERMATA;
                    else state = VisitState.CANCELLATA;
                }
            }
            case COMPLETA -> {
                if(enrolled < minParticipants & today.isBefore (deadline))
                    state = VisitState.PROPOSTA;
                else if(today.isEqual (deadline)){
                    if (enrolled >= minParticipants) state = VisitState.CONFERMATA;
                    else state = VisitState.CANCELLATA;
                }
            }
            case CONFERMATA -> {
                if (!today.isEqual (visitDate)) state = VisitState.EFFETTUATA;
            }

        }
    }

    public String getId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    public void setId(String id) {
        this.id = sanitizeId(id);
    }

    public List<PlannedVisit> generateOccurrences(YearMonth month) {
        Objects.requireNonNull(month, "Il mese di generazione non può essere nullo");
        ensureSchedulesInitialized();
        if (schedules.isEmpty()) {
            return List.of();
        }

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate effectiveStart = validFrom == null ? monthStart : (validFrom.isAfter(monthStart) ? validFrom : monthStart);
        LocalDate effectiveEnd = validTo == null ? monthEnd : (validTo.isBefore(monthEnd) ? validTo : monthEnd);
        if (effectiveStart.isAfter(effectiveEnd)) {
            return List.of();
        }

        return schedules.stream()
                .filter(Objects::nonNull)
                .flatMap(slot -> computeDatesForSlot(slot, monthStart, monthEnd).stream()
                        .filter(date -> !date.isBefore(effectiveStart) && !date.isAfter(effectiveEnd))
                        .map(date -> new PlannedVisit(date, cloneSlot(slot), getId(), true)))
                .sorted(Comparator.comparing(PlannedVisit::getDate)
                        .thenComparing(p -> p.getTimeSlot().getStartTime()))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), List::copyOf));
    }

    public boolean isValidOn(LocalDate date) {
        Objects.requireNonNull(date, "La data non può essere nulla");
        boolean afterStart = validFrom == null || !date.isBefore(validFrom);
        boolean beforeEnd = validTo == null || !date.isAfter(validTo);
        return afterStart && beforeEnd;
    }

    private TimeSlot cloneSlot(TimeSlot slot) {
        if (slot == null) {
            return null;
        }
        return new TimeSlot(slot.getDay(), slot.getStartTime(), slot.getDuration());
    }

    private List<LocalDate> computeDatesForSlot(TimeSlot slot, LocalDate monthStart, LocalDate monthEnd) {
        List<LocalDate> dates = new ArrayList<>();
        if (slot == null) {
            return dates;
        }
        LocalDate current = monthStart.with(TemporalAdjusters.nextOrSame(slot.getDay()));
        while (!current.isAfter(monthEnd)) {
            dates.add(current);
            current = current.with(TemporalAdjusters.next(slot.getDay()));
        }
        return dates;
    }


    public String getVisitTitle() {
        return visitTitle;
    }
    public void setVisitTitle(String visitTitle) {
        this.visitTitle = visitTitle;
    }
    public String getVisitDescription() {
        return visitDescription;
    }
    public void setVisitDescription(String visitDescription) {
        this.visitDescription = visitDescription;
    }
    public String getVisitMeetLocation() {
        return visitMeetLocation;
    }
    public void setVisitMeetLocation(String visitMeetLocation) {
        this.visitMeetLocation = visitMeetLocation;
    }
    public LocalDate getValidFrom() {
        return validFrom;
    }
    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }
    public LocalDate getValidTo() {
        return validTo;
    }
    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
    public List<TimeSlot> getSchedules() {
        ensureSchedulesInitialized();
        return schedules;
    }
    public void setSchedules(List<TimeSlot> schedules) {
        this.schedules = schedules == null ? new ArrayList<> () : new ArrayList<>(schedules);
    }
    public Boolean getTicketRequired() {
        return ticketRequired;
    }
    public void setTicketRequired(Boolean ticketRequired) {
        this.ticketRequired = ticketRequired;
    }
    public int getMinParticipants() {
        return minParticipants;
    }
    public void setMinParticipants(int minParticipants) {
        this.minParticipants = minParticipants;
    }
    public int getMaxParticipants() {
        return maxParticipants;
    }
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    public Place getPlace() {
        return place;
    }
    public void setPlace(Place place) {
        this.place = place;
    }
    public List<Volunteer> getGuides() {
        ensureGuidesInitialized();
        return guides;
    }
    public void setGuides(List<Volunteer> guides) {
        this.guides = guides == null ? new ArrayList<> () : new ArrayList<>(guides);
    }
    public VisitState getState() {
        return state;
    }
    public int getEnrolled() {
        return enrolled;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
        if (visitDate == null) {
            this.enrollmentDeadline = null;
        } else if (enrollmentDeadline == null || !enrollmentDeadline.isEqual(visitDate.minusDays(3))) {
            this.enrollmentDeadline = computeEnrollmentDeadline(visitDate);
        }
    }

    public LocalDate getEnrollmentDeadline() {
        if (enrollmentDeadline == null) {
            enrollmentDeadline = computeEnrollmentDeadline(visitDate);
        }
        return enrollmentDeadline;
    }

    public LocalDate computeEnrollmentDeadline(LocalDate visitDate) {
        Objects.requireNonNull(visitDate, "La data della visita non può essere nulla");
        return visitDate.minusDays(3);
    }


    private void ensureSchedulesInitialized() {
        if (schedules == null) {
            schedules = new ArrayList<>();
        }
    }

    private void ensureGuidesInitialized() {
        if (guides == null) {
            guides = new ArrayList<>();
        }
    }

    private String sanitizeId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return candidate.trim();
    }

}
