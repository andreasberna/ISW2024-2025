package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.VisitStatus;

import java.time.LocalDate;
import java.util.List;

public interface BeneficiaryService extends LoginService {

    void register (String fullName, String username, String password);

    List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus... status);

    String bookVisit(String username, String visitId, int participants, String ntoes, LocalDate today);

    List<VisitBookingDTO> listBookings(String username);

    void cancelBooking(String username, String bookingCode, LocalDate today);
}
