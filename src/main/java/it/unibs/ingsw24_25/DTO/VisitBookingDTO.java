package it.unibs.ingsw24_25.DTO;

import it.unibs.ingsw24_25.model.VisitStatus;

import java.time.LocalDate;
import java.util.Objects;

public class VisitBookingDTO {
    private final String code;
    private final String beneficiaryName;
    private final int participants;
    private final String notes;
    private final LocalDate visitDate;
    private final String visitTitle;
    private final VisitStatus status;

    public VisitBookingDTO(String code, String beneficiaryName, int participants, String notes,
                           LocalDate visitDate, String visitTitle, VisitStatus status) {
        this.code = Objects.requireNonNull(code);
        this.beneficiaryName = beneficiaryName == null ? "" : beneficiaryName;
        this.participants = participants;
        this.notes = notes == null ? "" : notes;
        this.visitDate = visitDate;
        this.visitTitle = visitTitle == null ? "" : visitTitle;
        this.status = status;
    }

    public String getCode() {return code;}
    public String getBeneficiaryName() {return beneficiaryName;}
    public int getParticipants() {return participants;}
    public String getNotes() {return notes;}
    public LocalDate getVisitDate() {return visitDate;}
    public String getVisitTitle() {return visitTitle;}
    public VisitStatus getStatus() {return status;}
}
