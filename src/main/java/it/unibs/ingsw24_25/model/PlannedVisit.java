package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlannedVisit {

    private LocalDate date;
    private TimeSlot timeSlot;
    private String visitTypeId;
    private List<String> assignedVolunteerIds = new ArrayList<>();
    private boolean proposable;

    public PlannedVisit(LocalDate date, TimeSlot timeSlot, String visitTypeId, boolean proposable) {
        this(date, timeSlot, visitTypeId, proposable, List.of());
    }

    public PlannedVisit(LocalDate date, TimeSlot timeSlot, String visitTypeId, boolean proposable, List<String> assignedVolunteerIds) {
        this.date = Objects.requireNonNull(date, "La data della visita non può essere nulla");
        this.timeSlot = Objects.requireNonNull(timeSlot, "Il time slot della visita non può essere nullo");
        this.visitTypeId = Objects.requireNonNull(visitTypeId, "L'identificativo del tipo visita non può essere nullo");
        this.proposable = proposable;
        setAssignedVolunteerIds(assignedVolunteerIds);
    }

    public LocalDate getDate() {
        return date;
    }
    public TimeSlot getTimeSlot() {
        return timeSlot;
    }
    public String getVisitTypeId() {
        return visitTypeId;
    }
    public boolean isProposable() {
        return proposable;
    }
    public void setProposable(boolean proposable) {
        this.proposable = proposable;
    }
    public List<String> getAssignedVolunteerIds() {
        return assignedVolunteerIds;
    }
    public void setAssignedVolunteerIds(List<String> assignedVolunteerIds) {
        if (assignedVolunteerIds == null) {
            this.assignedVolunteerIds = new ArrayList<>();
        } else {
            this.assignedVolunteerIds = new ArrayList<>(assignedVolunteerIds);
        }
    }
    public void assignVolunteer(String volunteerId) {
        Objects.requireNonNull (volunteerId, "L'id del volontario non può essere nullo");
        if (assignedVolunteerIds.contains(volunteerId))
            assignedVolunteerIds.add(volunteerId);
    }
    public boolean unassignVolunteer(String volunteerId) {
        return assignedVolunteerIds.remove(volunteerId);
    }
    public void clearAssignements(){
        assignedVolunteerIds.clear();
    }

}
