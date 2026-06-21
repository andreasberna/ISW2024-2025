package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.VisitStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface BeneficiaryService extends LoginService {

    @Transactional
    void register (String fullName, String username, String password);

    List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus... status);

    @Transactional
    String bookVisit(String username, String visitId, int participants, String notes, LocalDate today);

    List<VisitBookingDTO> listBookings(String username);

    @Transactional
    void cancelBooking(String username, String bookingCode, LocalDate today);

    @Transactional
    void changePassword(String username, String oldPassword, String newPassword);
}