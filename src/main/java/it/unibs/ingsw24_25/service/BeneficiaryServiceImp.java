package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class BeneficiaryServiceImp implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final VolunteerRepository volunteerRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final SettingsRepository settingsRepository;
    private final BeneficiaryBookingManager bookingManager;

    public BeneficiaryServiceImp(BeneficiaryRepository beneficiaryRepository,
                                 VolunteerRepository volunteerRepository,
                                 MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                 VisitTypeRepository visitTypeRepository,
                                 SettingsRepository settingsRepository) {
        this.beneficiaryRepository = Objects.requireNonNull(beneficiaryRepository);
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository);
        this.monthlyVisitPlanRepository = Objects.requireNonNull(monthlyVisitPlanRepository);
        this.visitTypeRepository = Objects.requireNonNull(visitTypeRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.bookingManager = new BeneficiaryBookingManager (
                this.beneficiaryRepository,
                this.volunteerRepository,
                this.monthlyVisitPlanRepository,
                this.visitTypeRepository,
                this.settingsRepository
        );
    }

    @Override
    public void register(String fullName, String username, String password) {
        String sanitizedUsername = requireNonBlank(username, "Lo username non può essere vuoto");
        if (beneficiaryRepository.findByUsername(sanitizedUsername).isPresent()
                || volunteerRepository.findByNickname(sanitizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username già utilizzato");
        }

        Beneficiary beneficiary = new Beneficiary(sanitizedUsername,
                requireNonBlank(password, "La password non può essere nulla"),
                requireNonBlank(fullName, "Il nome del fruitore non può essere vuoto"));
        beneficiaryRepository.save(beneficiary);
    }

    @Override
    public List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus... statuses) {
        return bookingManager.listVisitsByStatus(statuses);
    }

    @Override
    public String bookVisit(String username, String visitId, int participants, String notes, LocalDate today) {
        return  bookingManager.bookVisit(username, visitId, participants, notes, today);
    }

    @Override
    public List<VisitBookingDTO> listBookings(String username) {
        return bookingManager.listBookings(username);
    }

    @Override
    public void cancelBooking(String username, String bookingCode, LocalDate today) {
        bookingManager.cancelBooking(username, bookingCode, today);
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        if (nickname == null || nickname.isBlank()) {return false;}

        beneficiaryRepository.findByUsername (nickname.trim ())
                .orElseThrow(() -> new IllegalArgumentException ("Fruitore non trovato"));
        return false;
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        throw new UnsupportedOperationException("I fruitori non dispongono di credenziali predefinite");
    }

    @Override
    public void setPersonalCredentials(String nickname, String password) {
        throw new UnsupportedOperationException ("La gestione delle credenziali del fruitore è delegata al processo di registrazione");
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        throw new UnsupportedOperationException ("La gestione delle credenziali del fruitore è delegata al processo di registrazione");
    }

    @Override
    public boolean verifyLogin(String username, String password) {
        if(username == null || password == null) return false;

        return beneficiaryRepository.findByUsername(username.trim ())
                .map (beneficiary -> beneficiary.passwordMatches (password))
                .orElse(false);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

}
