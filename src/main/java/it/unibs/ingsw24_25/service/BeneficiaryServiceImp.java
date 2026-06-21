package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.Beneficiary;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.repository.BeneficiaryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BeneficiaryServiceImp implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryBookingManager bookingManager;
    private final PasswordEncoder passwordEncoder;

    public BeneficiaryServiceImp(BeneficiaryRepository beneficiaryRepository,
                                 BeneficiaryBookingManager bookingManager,
                                 PasswordEncoder passwordEncoder) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.bookingManager = bookingManager;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuovo fruitore (beneficiario) nel sistema.
     * Crea l'account codificando la password in modo sicuro.
     *
     * @param fullName Il nome completo del beneficiario.
     * @param username Lo username univoco scelto.
     * @param password La password in chiaro, che verrà codificata.
     * @throws IllegalArgumentException se lo username è già in uso o se i parametri sono vuoti.
     */
    @Override
    @Transactional
    public void register(String fullName, String username, String password) {
        String sanitizedUsername = requireNonBlank(username, "Lo username non può essere vuoto");
        if (beneficiaryRepository.findByUsername(sanitizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username già utilizzato");
        }

        Beneficiary beneficiary = new Beneficiary(sanitizedUsername,
                passwordEncoder.encode(requireNonBlank(password, "La password non può essere nulla")),
                requireNonBlank(fullName, "Il nome del fruitore non può essere vuoto"));
        beneficiaryRepository.save(beneficiary);
    }

    /**
     * Elenca le occorrenze delle visite filtrandole per stato (es. CONFIRMED, PLANNED).
     *
     * @param statuses Uno o più stati per cui filtrare le visite.
     * @return Lista di DTO che rappresentano le occorrenze delle visite trovate.
     */
    @Override
    public List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus... statuses) {
        return bookingManager.listVisitsByStatus(statuses);
    }

    /**
     * Permette a un beneficiario di prenotare dei posti per una specifica visita guidata.
     *
     * @param username Lo username del beneficiario che effettua la prenotazione.
     * @param visitId L'identificativo univoco della visita.
     * @param participants Il numero di persone incluse nella prenotazione.
     * @param notes Eventuali note aggiuntive.
     * @param today La data odierna, per verificare i vincoli temporali della prenotazione.
     * @return Il codice univoco generato per la prenotazione.
     * @throws IllegalArgumentException se i parametri non sono validi o se non ci sono posti sufficienti.
     * @throws EntityNotFoundException se la visita non esiste.
     */
    @Override
    @Transactional
    public String bookVisit(String username, String visitId, int participants, String notes, LocalDate today) {
        return bookingManager.bookVisit(username, visitId, participants, notes, today);
    }

    /**
     * Restituisce la lista di tutte le prenotazioni effettuate da un beneficiario.
     *
     * @param username Lo username del beneficiario.
     * @return Lista di DTO che rappresentano le prenotazioni.
     */
    @Override
    public List<VisitBookingDTO> listBookings(String username) {
        return bookingManager.listBookings(username);
    }

    /**
     * Annulla una prenotazione esistente.
     *
     * @param username Lo username del beneficiario titolare della prenotazione.
     * @param bookingCode Il codice univoco della prenotazione.
     * @param today La data odierna, per verificare se l'annullamento è ancora consentito.
     * @throws IllegalArgumentException se l'annullamento non è più consentito (es. troppo a ridosso della visita).
     * @throws EntityNotFoundException se la prenotazione non esiste.
     */
    @Override
    @Transactional
    public void cancelBooking(String username, String bookingCode, LocalDate today) {
        bookingManager.cancelBooking(username, bookingCode, today);
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        if (nickname == null || nickname.isBlank()) {return false;}

        beneficiaryRepository.findByUsername(nickname.trim())
                .orElseThrow(() -> new EntityNotFoundException("Fruitore non trovato"));
        return false;
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        throw new UnsupportedOperationException("I fruitori non dispongono di credenziali predefinite");
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        throw new UnsupportedOperationException("La gestione delle credenziali del fruitore è delegata al processo di registrazione");
    }

    /**
     * Verifica le credenziali di accesso di un beneficiario.
     * Utilizza l'hashing sicuro (BCrypt) per il confronto della password.
     *
     * @param username L'username fornito in fase di login.
     * @param password La password in chiaro da verificare.
     * @return true se le credenziali sono valide, false altrimenti.
     */
    @Override
    public boolean verifyLogin(String username, String password) {
        if(username == null || password == null) return false;

        return beneficiaryRepository.findByUsername(username.trim())
                .map(beneficiary -> beneficiary.passwordMatches(password, passwordEncoder))
                .orElse(false);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Beneficiary beneficiary = beneficiaryRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Beneficiary not found"));
        if (!passwordEncoder.matches(oldPassword, beneficiary.getPassword())) {
            throw new IllegalArgumentException("Old password does not match");
        }
        beneficiary.setPassword(passwordEncoder.encode(newPassword));
        beneficiaryRepository.save(beneficiary);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}