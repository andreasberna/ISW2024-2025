package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.DTOMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class BeneficiaryServiceImp implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final VolunteerRepository volunteerRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final SettingsRepository settingsRepository;

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
    }

    @Override
    public void register(String fullName, String username, String password) {
        String sanitizedUsername = requireNonBlank(username, "Lo username non può essere vuoto");
        if (beneficiaryRepository.findByUsername(sanitizedUsername).isPresent() || volunteerRepository.findByNickname (sanitizedUsername).isPresent()) {
            throw new IllegalArgumentException ("Username già utilizato");
        }

        Beneficiary beneficiary = new Beneficiary(sanitizedUsername,
                requireNonBlank(password, "La password non può essere nulla"),
                requireNonBlank(fullName, "Il nome del fruitore non può essere vuoto"));
        beneficiaryRepository.save(beneficiary);
    }

    @Override
    public List<VisitOccurrenceDTO> listVisitsByStatus(VisitStatus... statuses) {
        Set<VisitStatus> wanted = statuses == null || statuses.length == 0
                ? EnumSet.allOf (VisitStatus.class)
                : EnumSet.copyOf(Arrays.asList(statuses));
        Map<String, VisitType> visitTypes = visitTypeRepository.findAll ().stream ()
                .filter (Objects::nonNull)
                .collect (Collectors.toMap (VisitType::getId, visit -> visit, (left, right) -> left));
        List<VisitOccurrenceDTO> occurrences = new ArrayList<>();
        for (MonthlyVisitPlan plan : monthlyVisitPlanRepository.findAll()) {
            YearMonth month = plan.getTargetMonth ();
            for (PlannedVisit visit : plan.getPlannedVisits()) {
                if (visit != null || !wanted.contains (visit.getStatus ())) continue;
                VisitType visitType = visitTypes.get (visit.getVisitTypeId ());
                occurrences.add(DTOMapper.toVisitOccurrenceDTO (plan, visit, visitType, false));
            }
        }
        occurrences.sort(Comparator.comparing(VisitOccurrenceDTO::getDate).
                thenComparing (VisitOccurrenceDTO::getStartTime, Comparator.nullsLast (Comparator.naturalOrder())));
        return occurrences;
    }

    @Override
    public String bookVisit(String username, String visitId, int participants, String notes, LocalDate today) {
        Beneficiary beneficiary = beneficiaryRepository.findByUsername(requireNonBlank(username, "Username non valido"))
                .orElseThrow(() -> new IllegalArgumentException("Fruitore non trovato"));
        Objects.requireNonNull(today, "La data odierna non può essere nulla");
        int maxPerSubscription = resolveMaxPeoplePerSubscription();
        if (participants <= 0 || participants > maxPerSubscription) {
            throw new IllegalArgumentException("Numero partecipanti non valido");
        }
        PlannedVisitWithPlan target = findVisitAcrossPlans(visitId);
        PlannedVisit plannedVisit = target.visit;
        if (!plannedVisit.isProposable()) {
            throw new IllegalStateException("La visita non è al momento prenotabile");
        }
        if (plannedVisit.getStatus() == VisitStatus.CANCELLED) {
            throw new IllegalStateException("La visita è stata annullata");
        }
        VisitType visitType = resolveVisitType(plannedVisit.getVisitTypeId());
        int newTotal = plannedVisit.getBookedParticipants() + participants;
        if (newTotal > visitType.getMaxParticipants()) {
            throw new IllegalStateException("Numero massimo di partecipanti superato");
        }
        VisitBooking booking = new VisitBooking(beneficiary.getUsername(), beneficiary.getFullName(), participants, notes);
        plannedVisit.addBooking(booking);
        updateVisitStatus(plannedVisit, visitType);
        if (plannedVisit.getBookedParticipants() >= visitType.getMaxParticipants()) {
            plannedVisit.setProposable(false);
        }
        persistPlan(target.plan);
        return booking.getCode();
    }

    @Override
    public List<VisitBookingDTO> listBookings(String username) {
        String sanitized = requireNonBlank(username, "Username non valido");
        beneficiaryRepository.findByUsername(sanitized)
                .orElseThrow(() -> new IllegalArgumentException("Fruitore non trovato"));
        List<VisitBookingDTO> bookings = new ArrayList<>();
        for (MonthlyVisitPlan plan : monthlyVisitPlanRepository.findAll()) {
            for (PlannedVisit visit : plan.getPlannedVisits()) {
                VisitType visitType = visitTypeRepository.findById(visit.getVisitTypeId()).orElse(null);
                for (VisitBooking booking : visit.getBookings()) {
                    if (booking.getBeneficiaryUsername().equals(sanitized)) {
                        bookings.add(new VisitBookingDTO(
                                booking.getCode(),
                                booking.getBeneficiaryName(),
                                booking.getParticipants(),
                                booking.getNotes(),
                                visit.getDate(),
                                visitType != null ? visitType.getVisitTitle() : visit.getVisitTypeId(),
                                visit.getStatus()
                        ));
                    }
                }
            }
        }
        return bookings;
    }

    @Override
    public void cancelBooking(String username, String bookingCode, LocalDate today) {
        String sanitizedUsername = requireNonBlank(username, "Username non valido");
        beneficiaryRepository.findByUsername(sanitizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Fruitore non trovato"));
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new IllegalArgumentException("Codice prenotazione non valido");
        }
        Objects.requireNonNull(today, "La data odierna non può essere nulla");
        PlannedVisitWithPlan target = findVisitByBooking(bookingCode);
        VisitBooking booking = target.visit.findBookingByCode(bookingCode);
        if (booking == null) {
            throw new IllegalArgumentException("Prenotazione non trovata");
        }
        if (!sanitizedUsername.equals(booking.getBeneficiaryUsername())) {
            throw new IllegalStateException("Il fruitore non è proprietario della prenotazione");
        }
        if (target.visit.getStatus() == VisitStatus.CANCELLED) {
            throw new IllegalStateException("Impossibile cancellare una prenotazione su visita annullata");
        }
        target.visit.removeBookingByCode(bookingCode);
        VisitType visitType = resolveVisitType(target.visit.getVisitTypeId());
        updateVisitStatus(target.visit, visitType);
        if (target.visit.getBookedParticipants() < visitType.getMaxParticipants()) {
            target.visit.setProposable(true);
        }
        persistPlan(target.plan);
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

    private PlannedVisitWithPlan findVisitAcrossPlans(String visitId) {
        String sanitized = requireNonBlank(visitId, "Identificativo visita non valido");
        for (MonthlyVisitPlan plan : monthlyVisitPlanRepository.findAll()) {
            Optional<PlannedVisit> match = plan.findVisitById(sanitized);
            if (match.isPresent()) {
                return new PlannedVisitWithPlan(plan, match.get());
            }
        }
        throw new IllegalArgumentException("Visita non trovata: " + sanitized);
    }

    private PlannedVisitWithPlan findVisitByBooking(String bookingCode) {
        for (MonthlyVisitPlan plan : monthlyVisitPlanRepository.findAll()) {
            for (PlannedVisit visit : plan.getPlannedVisits()) {
                if (visit.findBookingByCode(bookingCode) != null) {
                    return new PlannedVisitWithPlan(plan, visit);
                }
            }
        }
        throw new IllegalArgumentException("Prenotazione non trovata");
    }

    private void updateVisitStatus(PlannedVisit visit, VisitType visitType) {
        int total = visit.getBookedParticipants();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return;
        }
        if (total >= visitType.getMinParticipants()) {
            visit.setStatus(VisitStatus.CONFIRMED);
        } else {
            visit.setStatus(VisitStatus.PROPOSED);
        }
    }

    private int resolveMaxPeoplePerSubscription() {
        return settingsRepository.load()
                .map(SystemSettings::getMaxPeoplePerSubscription)
                .orElse(1);
    }

    private VisitType resolveVisitType(String visitTypeId) {
        return visitTypeRepository.findById(visitTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo visita non trovato: " + visitTypeId));
    }

    private void persistPlan(MonthlyVisitPlan plan) {
        monthlyVisitPlanRepository.save(plan);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private record PlannedVisitWithPlan(MonthlyVisitPlan plan, PlannedVisit visit) {}
}
