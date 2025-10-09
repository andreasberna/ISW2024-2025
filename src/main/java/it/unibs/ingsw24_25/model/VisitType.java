package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.util.List;

public class VisitType {
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
    private LocalDate enrollmentDeadline= visitDate.minusDays(3);
    private int enrolled;

    public VisitType(String visitTitle, String visitDescription, String visitMeetLocation,
                     LocalDate validFrom, LocalDate validTo, List<TimeSlot> schedules,
                     Boolean ticketRequired, int minParticipants, int maxParticipants,
                     Place place, List<Volunteer> guides ) {
        this.visitTitle = visitTitle;
        this.visitDescription = visitDescription;
        this.visitMeetLocation = visitMeetLocation;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.schedules = schedules;
        this.ticketRequired = ticketRequired;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.place = place;
        this.guides = guides;
    }

    public void addSchedule(TimeSlot timeSlot){
        this.schedules.add(timeSlot);
    }
    public void removeSchedule(TimeSlot timeSlot){
        this.schedules.remove(timeSlot);
    }
    public void addGuide(Volunteer volunteer){
        this.guides.add(volunteer);
    }
    public void removeGuide(Volunteer volunteer){
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
        return schedules;
    }
    public void setSchedules(List<TimeSlot> schedules) {
        this.schedules = schedules;
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
        return guides;
    }
    public void setGuides(List<Volunteer> guides) {
        this.guides = guides;
    }
    public VisitState getState() {
        return state;
    }
    public int getEnrolled() {
        return enrolled;
    }

}
