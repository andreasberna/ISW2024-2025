package it.unibs.ingsw24_25.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

public class PlannedVisitDTO {

    private Long id;
    private String visitTypeTitle;
    private String volunteerNickname;
    private LocalDate visitDate;
    private LocalTime visitTime;
    private String status;

    public PlannedVisitDTO(Long id, String visitTypeTitle, String volunteerNickname, LocalDate visitDate, LocalTime visitTime, String status) {
        this.id = id;
        this.visitTypeTitle = visitTypeTitle;
        this.volunteerNickname = volunteerNickname;
        this.visitDate = visitDate;
        this.visitTime = visitTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getVisitTypeTitle() {
        return visitTypeTitle;
    }

    public String getVolunteerNickname() {
        return volunteerNickname;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public LocalTime getVisitTime() {
        return visitTime;
    }

    public String getStatus() {
        return status;
    }
}
