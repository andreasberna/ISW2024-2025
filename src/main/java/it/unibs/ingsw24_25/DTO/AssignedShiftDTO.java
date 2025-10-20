package it.unibs.ingsw24_25.DTO;

import it.unibs.ingsw24_25.model.TimeSlot;

import java.time.LocalDate;
import java.time.YearMonth;

public class AssignedShiftDTO {

    private YearMonth month;
    private LocalDate date;
    private String visitTypeId;
    private TimeSlot slot;

    public AssignedShiftDTO(YearMonth month, LocalDate date, String visitTypeId, TimeSlot slot) {
        this.month = month;
        this.date = date;
        this.visitTypeId = visitTypeId;
        this.slot = slot;
    }

    public YearMonth getMonth() {
        return month;
    }
    public LocalDate getDate() {
        return date;
    }
    public String getVisitTypeId() {
        return visitTypeId;
    }
    public TimeSlot getSlot() {
        return slot;
    }

}
