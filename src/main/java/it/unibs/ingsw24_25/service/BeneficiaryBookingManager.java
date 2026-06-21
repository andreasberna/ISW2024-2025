package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.DTOMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
public class BeneficiaryBookingManager implements BeneficiaryService {
    private static final Logger log = LoggerFactory.getLogger(BeneficiaryBookingManager.class);

    private final BeneficiaryRepository beneficiaryRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final PlannedVisitRepository plannedVisitRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    public BeneficiaryBookingManager(BeneficiaryRepository beneficiaryRepository,
                                     MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                     VisitTypeRepository visitTypeRepository,
                                     PlannedVisitRepository plannedVisitRepository,
                                     SettingsRepository settingsRepository,
                                     PasswordEncoder passwordEncoder) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.monthlyVisitPlanRepository = monthlyVisitPlanRepository;
        this.visitTypeRepository = visitTypeRepository;
        this.plannedVisitRepository = plannedVisitRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean verifyLogin(String username, String password) {
        return beneficiaryRepository.findByUsername(username)
                .map(beneficiary -> passwordEncoder.matches(password, beneficiary.getPassword()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void register(String fullName, String username, String password) {
        if (beneficiaryRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        Beneficiary beneficiary = new Beneficiary(username, passwordEncoder.encode(password), fullName);
        beneficiaryRepository.save(beneficiary);
    }

    @Override
    public List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus... statuses) {
        Set<VisitStatus> wanted = statuses == null || statuses.length == 0
                ? EnumSet.allOf(VisitStatus.class)
                : EnumSet.copyOf(Arrays.asList(statuses));

        List<VisitOccurrenceDTO> occurrences = new ArrayList<>();
        for (MonthlyVisitPlan plan : monthlyVisitPlanRepository.findAll()) {
            if (plan.getPhase() == PlanningPhase.PUBBLICATO) {
                for (PlannedVisit visit : plan.getVisits()) {
                    if (visit.getVisitType() == null) continue;
                    if (wanted.contains(visit.getStatus())) {
                        occurrences.add(DTOMapper.toVisitOccurrenceDTO(plan, visit, visit.getVisitType(), false));
                    }
                }
            }
        }
        occurrences.sort(Comparator.comparing(VisitOccurrenceDTO::getDate)
                .thenComparing(VisitOccurrenceDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return occurrences;
    }

    @Override
    @Transactional
    public String bookVisit(String username, String visitId, int participants, String notes, LocalDate today) {
        Beneficiary beneficiary = beneficiaryRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Beneficiary not found: " + username));

        PlannedVisit visit = plannedVisitRepository.findById(Long.parseLong(visitId))
                .orElseThrow(() -> new EntityNotFoundException("Planned visit not found: " + visitId));

        if (visit.getStatus() != VisitStatus.PROPOSTA) {
            throw new IllegalStateException("Visita non disponibile per la prenotazione: " + visit.getStatus());
        }

        // Verify participants count against system limit (null-safe)
        SystemSettings settings = settingsRepository.findById(1L).orElse(null);
        int maxPerSub = (settings != null && settings.getMaxPeoplePerSubscription() > 0)
                ? settings.getMaxPeoplePerSubscription() : Integer.MAX_VALUE;
        if (participants < 1 || participants > maxPerSub) {
            throw new IllegalArgumentException("Numero partecipanti non valido: deve essere tra 1 e " + maxPerSub);
        }

        // Check available spots
        int currentBooked = visit.countBookedParticipants();
        int maxParticipants = visit.getVisitType().getMaxParticipants();
        if (currentBooked + participants > maxParticipants) {
            throw new IllegalStateException("Posti insufficienti: disponibili " + (maxParticipants - currentBooked));
        }

        // Create and add booking
        VisitBooking booking = new VisitBooking(
                beneficiary.getUsername(),
                beneficiary.getFullName(),
                participants,
                notes
        );
        visit.addBooking(booking);
        log.info("Prenotazione {} creata da {} per visita {} ({} partecipanti)",
                booking.getCode(), username, visitId, participants);

        // Transition to COMPLETA if now fully booked
        if (currentBooked + participants >= maxParticipants) {
            visit.setStatus(VisitStatus.COMPLETA);
            log.info("Visita {} raggiunge capienza massima, passata a COMPLETA", visitId);
        }

        plannedVisitRepository.save(visit);
        return booking.getCode();
    }

    @Override
    public List<VisitBookingDTO> listBookings(String username) {
        beneficiaryRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Beneficiario non trovato: " + username));

        List<VisitBookingDTO> result = new ArrayList<>();
        for (MonthlyVisitPlan plan : monthlyVisitPlanRepository.findAll()) {
            for (PlannedVisit visit : plan.getVisits()) {
                for (VisitBooking booking : visit.getBookings()) {
                    if (booking.getBeneficiaryUsername().equals(username)) {
                        result.add(new VisitBookingDTO(
                                booking.getCode(),
                                booking.getBeneficiaryName(),
                                booking.getParticipants(),
                                booking.getNotes(),
                                visit.getVisitDate(),
                                visit.getVisitType().getVisitTitle(),
                                visit.getStatus()
                        ));
                    }
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void cancelBooking(String username, String bookingCode, LocalDate today) {
        List<PlannedVisit> allVisits = plannedVisitRepository.findAll();

        for (PlannedVisit visit : allVisits) {
            Optional<VisitBooking> found = visit.getBookings().stream()
                    .filter(b -> b.getCode().equals(bookingCode))
                    .findFirst();

            if (found.isPresent()) {
                VisitBooking booking = found.get();
                if (!booking.getBeneficiaryUsername().equals(username)) {
                    throw new IllegalStateException("Non autorizzato a cancellare questa prenotazione");
                }
                if (visit.getStatus() == VisitStatus.CONFERMATA || visit.getStatus() == VisitStatus.CANCELLATA) {
                    throw new IllegalStateException("Impossibile cancellare: visita già " + visit.getStatus());
                }
                boolean wasCompleta = visit.getStatus() == VisitStatus.COMPLETA;
                visit.removeBooking(bookingCode);
                // Revert to PROPOSTA if visit was full and now has room again
                if (wasCompleta && visit.countBookedParticipants() < visit.getVisitType().getMaxParticipants()) {
                    visit.setStatus(VisitStatus.PROPOSTA);
                }
                plannedVisitRepository.save(visit);
                log.info("Prenotazione {} cancellata da {}", bookingCode, username);
                return;
            }
        }
        throw new EntityNotFoundException("Prenotazione non trovata: " + bookingCode);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Beneficiary beneficiary = beneficiaryRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Beneficiary not found: " + username));

        if (!passwordEncoder.matches(oldPassword, beneficiary.getPassword())) {
            throw new IllegalArgumentException("Old password is not correct");
        }

        beneficiary.setPassword(passwordEncoder.encode(newPassword));
        beneficiaryRepository.save(beneficiary);
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        return false;
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
    }
}