package it.unibs.ingsw24_25.controller;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.service.BeneficiaryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = Objects.requireNonNull(beneficiaryService, "beneficiaryService non può essere nullo");
    }

    public void register(String fullName, String username, String password) {
        beneficiaryService.register(fullName, username, password);
    }

    public boolean verifyLogin(String username, String password) {
        Objects.requireNonNull(username, "username non può essere nullo");
        Objects.requireNonNull(password, "password non può essere nullo");
        return beneficiaryService.verifyLogin(username, password);
    }

    public List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus status) {
        Objects.requireNonNull(status, "status non può essere nullo");
        return beneficiaryService.listVisitsByStatus(status);
    }

    public String bookVisit(String username, String visitId, int participants, String notes, LocalDate date) {
        Objects.requireNonNull(username, "username non può essere nullo");
        Objects.requireNonNull(visitId, "visitId non può essere nullo");
        Objects.requireNonNull(date, "date non può essere nullo");
        return beneficiaryService.bookVisit(username, visitId, participants, notes, date);
    }

    public List<VisitBookingDTO> listBookings(String username) {
        Objects.requireNonNull(username, "username non può essere nullo");
        return beneficiaryService.listBookings(username);
    }

    public void cancelBooking(String username, String code, LocalDate date) {
        Objects.requireNonNull(username, "username non può essere nullo");
        Objects.requireNonNull(code, "code non può essere nullo");
        Objects.requireNonNull(date, "date non può essere nullo");
        beneficiaryService.cancelBooking(username, code, date);
    }
}
