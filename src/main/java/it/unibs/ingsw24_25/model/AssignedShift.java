package it.unibs.ingsw24_25.model;

import java.time.LocalDate;
import java.util.Objects;

public class AssignedShift {

    private LocalDate date;
    private String visitTypeId;
    private TimeSlot slot;

    public AssignedShift(LocalDate date, String visitTypeId, TimeSlot slot) {
        if (date == null) {
            throw new NullPointerException("La data del turno non può essere nulla");
        }
        if (slot == null) {
            throw new NullPointerException("La fascia oraria non può essere nulla");
        }
        this.date = date;
        this.visitTypeId = requireNonBlank(visitTypeId, "L'identificativo del tipo visita non può essere nullo");
        this.slot = slot;
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
