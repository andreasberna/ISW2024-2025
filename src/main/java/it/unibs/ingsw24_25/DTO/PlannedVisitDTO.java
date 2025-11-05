package it.unibs.ingsw24_25.DTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public class PlannedVisitDTO {

    private YearMonth month;
    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private long durationMinutes;
    private String visitTypeId;
    private String visitTitle;
    private boolean proposable;
    private List<String> assignedVolunteers;

    public PlannedVisitDTO(YearMonth month,
                           LocalDate date,
                           DayOfWeek dayOfWeek,
                           LocalTime startTime,
                           long durationMinutes,
                           String visitTypeId,
                           String visitTitle,
                           boolean proposable,
                           List<String> assignedVolunteers) {
        this.month = month;
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.visitTypeId = visitTypeId;
        this.visitTitle = visitTitle;
        this.proposable = proposable;
        this.assignedVolunteers = assignedVolunteers;
    }

    public YearMonth getMonth() {
        return month;
    }

    public LocalDate getDate() {
        return date;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public String getVisitTypeId() {
        return visitTypeId;
    }

    public String getVisitTitle() {
        return visitTitle;
    }

    public boolean isProposable() {
        return proposable;
    }

    public List<String> getAssignedVolunteers() {
        return assignedVolunteers;
    }
}
