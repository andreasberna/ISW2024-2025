package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.util.Objects;

public class AssignedShift {

    private LocalDate date;
    private String visitTypeId;
    private TimeSlot slot;

    public AssignedShift(LocalDate date, String visitTypeId, TimeSlot slot) {
        this.date = Objects.requireNonNull(date);
        this.visitTypeId = Objects.requireNonNull(visitTypeId);
        this.slot = Objects.requireNonNull(slot);
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

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
