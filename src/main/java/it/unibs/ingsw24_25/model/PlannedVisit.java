package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlannedVisit {

    private String id;
    private LocalDate date;
    private TimeSlot timeSlot;
    private String visitTypeId;
    private List<String> assignedVolunteerIds = new ArrayList<>();
    private boolean proposable;
    private VisitStatus status = VisitStatus.PROPOSED;
    private List<VisitBooking> bookings = new ArrayList<> ();

    public PlannedVisit(LocalDate date, TimeSlot timeSlot, String visitTypeId, boolean proposable) {
        this(null, date, timeSlot, visitTypeId, proposable, List.of(), VisitStatus.PROPOSED, List.of());
    }

    public PlannedVisit(String id, LocalDate date, TimeSlot timeSlot,
                        String visitTypeId, boolean proposable, List<String> assignedVolunteerIds,
                        VisitStatus status, List<VisitBooking> bookings) {
        this.id = sanitizedId(id);
        this.date = Objects.requireNonNull(date, "La data della visita non può essere nulla");
        this.timeSlot = Objects.requireNonNull(timeSlot, "Il time slot della visita non può essere nullo");
        this.visitTypeId = Objects.requireNonNull(visitTypeId, "L'identificativo del tipo visita non può essere nullo");
        this.proposable = proposable;
        this.status = status == null ? VisitStatus.PROPOSED : status;
        setAssignedVolunteerIds(assignedVolunteerIds);
        setBookings(bookings);
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = sanitizedId(id);
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
    public boolean isProposable() {return proposable;}
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
        if (!assignedVolunteerIds.contains(volunteerId))
            assignedVolunteerIds.add(volunteerId);
    }
    public boolean unassignVolunteer(String volunteerId) {
        return assignedVolunteerIds.remove(volunteerId);
    }
    public void clearAssignements(){
        assignedVolunteerIds.clear();
    }
    public VisitStatus getStatus(){
        return status;
    }
    public void setStatus(VisitStatus status){
        this.status = status == null ? VisitStatus.PROPOSED : status;
    }
    public List<VisitBooking> getBookings(){
        return List.copyOf(bookings);
    }
    public void setBookings(List<VisitBooking> bookings){
        this.bookings = new ArrayList<> ();
        if (bookings == null) return;

        for (VisitBooking booking : bookings){
            if (booking != null) this.bookings.add(copyBooking(booking));
        }
    }

    public void addBooking(VisitBooking booking){
        Objects.requireNonNull (booking, "la prenotazione non può essere nulla");
        this.bookings.add(copyBooking(booking));
    }
    public boolean removeBookingByCode(String bookingCode){
        if (bookingCode == null) return false;

        return bookings.removeIf(booking -> bookingCode.equals (booking.getCode()));
    }
    public int getBookedParticipants(){
        return bookings.stream ()
                .mapToInt (VisitBooking::getParticipants)
                .sum ();
    }
    public VisitBooking findBookingByCode(String code){
        if (code == null) return null;

        return bookings.stream()
                .filter(booking -> code.equals (booking.getCode()))
                .findFirst ()
                .map (this::copyBooking)
                .orElse (null);
    }

    private VisitBooking copyBooking(VisitBooking booking){
        return new VisitBooking(
                booking.getCode(),
                booking.getBeneficiaryUsername(),
                booking.getBeneficiaryName(),
                booking.getParticipants(),
                booking.getNotes()
        );
    }

    private String sanitizedId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return java.util.UUID.randomUUID().toString();
        }
        return candidate.trim();
    }
}
