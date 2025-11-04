package it.unibs.ingsw24_25.DTO;

import it.unibs.ingsw24_25.model.VisitStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public class VisitOccurenceDTO {

    private final String id;
    private final YearMonth month;
    private final LocalDate date;
    private final LocalTime startTime;
    private final String title;
    private final String description;
    private final String meetingPoint;
    private final boolean ticketRequired;
    private final int minParticipants;
    private final int maxParticipants;
    private final int bookedParticipants;
    private final VisitStatus status;
    private final String visitTypeId;
    private final List<VisitBookingDTO> bookings;

    public VisitOccurenceDTO(String id,
                             YearMonth month,
                             LocalDate date,
                             LocalTime startTime,
                             String title,
                             String description,
                             String meetingPoint,
                             boolean ticketRequired,
                             int minParticipants,
                             int maxParticipants,
                             int bookedParticipants,
                             VisitStatus status,
                             String visitTypeId,
                             List<VisitBookingDTO> bookings) {
        this.id = id;
        this.month = month;
        this.date = date;
        this.startTime = startTime;
        this.title = title;
        this.description = description;
        this.meetingPoint = meetingPoint;
        this.ticketRequired = ticketRequired;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.bookedParticipants = bookedParticipants;
        this.status = status;
        this.visitTypeId = visitTypeId;
        this.bookings = bookings;
    }

    public String getId() {
        return id;
    }

    public YearMonth getMonth() {
        return month;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getMeetingPoint() {
        return meetingPoint;
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

    public int getBookedParticipants() {
        return bookedParticipants;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public String getVisitTypeId() {
        return visitTypeId;
    }

    public List<VisitBookingDTO> getBookings() {
        return bookings;
    }
}
