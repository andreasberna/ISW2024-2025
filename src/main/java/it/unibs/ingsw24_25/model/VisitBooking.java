package it.unibs.ingsw24_25.model;

import java.util.UUID;

public class VisitBooking {
    private String code;
    private String beneficiaryUsername;
    private String beneficiaryName;
    private int participants;
    private String notes;

    public VisitBooking(String beneficiaryUsername, String beneficiaryName, int participants, String notes) {
        this(UUID.randomUUID().toString(), beneficiaryUsername, beneficiaryName, participants, notes);
    }

    public VisitBooking(String code, String beneficiaryUsername, String beneficiaryName, int participants, String notes) {
        this.code = sanitizeCode(code);
        this.beneficiaryUsername = requireNonBlank(beneficiaryUsername, "Lo username del fruitore non può essere vuoto");
        this.beneficiaryName = requireNonBlank(beneficiaryName, "Il nome del fruitore non può essere vuoto");
        if (participants <= 0) {
            throw new IllegalArgumentException("Il numero di partecipanti deve essere positivo");
        }
        this.participants = participants;
        this.notes = notes == null ? "" : notes.trim();
    }

    public String getCode() {
        return code;
    }

    public String getBeneficiaryUsername() {
        return beneficiaryUsername;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public int getParticipants() {
        return participants;
    }

    public String getNotes() {
        return notes;
    }

    private String requireNonBlank(String value, String message){
        if (value == null || value.isBlank ())
            throw new IllegalArgumentException(message);
        return value.trim();
    }
    private String sanitizeCode(String value){
        if (value == null || value.isBlank ())
            return UUID.randomUUID().toString();
        return value.trim();
    }

}
