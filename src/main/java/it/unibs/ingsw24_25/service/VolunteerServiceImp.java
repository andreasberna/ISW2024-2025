package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.DTOMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class VolunteerServiceImp implements VolunteerService{

    private final VolunteerRepository volunteerRepository;
    private final SettingsRepository settingsRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final VolunteerCredentialManager credentialManager;

    public VolunteerServiceImp(VolunteerRepository volunteerRepository,
                               SettingsRepository settingsRepository,
                               MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                               VisitTypeRepository visitTypeRepository,
                               ProvisionedCredentialsRepository provisionedCredentialsRepository) {
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.monthlyVisitPlanRepository = Objects.requireNonNull(monthlyVisitPlanRepository);
        this.visitTypeRepository = Objects.requireNonNull(visitTypeRepository);
        this.credentialManager = new VolunteerCredentialManager(
                this.volunteerRepository,
                Objects.requireNonNull(provisionedCredentialsRepository)
        );
    }


    @Override
    public void submitAvailability(String nickname, MonthlyAvailability availability, LocalDate today) {
        Objects.requireNonNull (availability);
        Objects.requireNonNull(today);
        Volunteer volunteer = loadVolunteer(nickname);
        availability.ensureConsistency ();
        MonthlyAvailability sanitized = new MonthlyAvailability (
                availability.getReferenceMonth (),
                availability.getPreferredDays (),
                availability.getWeeklyFrequency (),
                availability.getSubmittedOn ()
        );
        ensureAvailabilityNotOnBlackoutDates(sanitized);
        ensureSubmissionAllowed(sanitized.getReferenceMonth ());
        volunteer.registerAvailability (sanitized, today);
        volunteerRepository.save(volunteer);
    }

    @Override
    public Optional<MonthlyAvailability> loadAvailability(String nickname, YearMonth month) {
        Objects.requireNonNull (month);
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.findAvailability (month);
    }

    @Override
    public List<AssignedShift> loadSchedule(String nickname, YearMonth month) {
        Objects.requireNonNull (month);
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.getShiftsForMonth (month);
    }

    @Override
    public void assignShifts(String nickname, YearMonth month, List<AssignedShift> shifts) {
        Objects.requireNonNull (month);
        Objects.requireNonNull (shifts);
        Volunteer volunteer = loadVolunteer(nickname);
        volunteer.assignShifts (month, shifts);
        volunteerRepository.save(volunteer);
    }

    @Override
    public Map<String, MonthlyAvailability> snapshotAvailabilities(YearMonth month, LocalDate capturedOn) {
        Objects.requireNonNull (month, "Il mese di riferimento non può essere nullo");
        LocalDate snapshotDate = capturedOn == null ? LocalDate.now() : capturedOn;
        Map<String, MonthlyAvailability> snapshots = new HashMap<> ();
        for (Volunteer volunteer : volunteerRepository.findAll()) {
            if (volunteer == null || !volunteer.isActive ()) continue;

            volunteer.findAvailability (month)
                    .map (availability -> availability.createSnapshot (snapshotDate))
                    .ifPresent (snapshot -> snapshots.put(volunteer.getNickname(), snapshot));
        }
        return snapshots;
    }

    @Override
    public void removeVolunteerAccount(String nickname) {
        credentialManager.removeVolunteerAccount(nickname);
    }
    @Override
    public List<VisitOccurrenceDTO> loadConfirmedGuidedVisits(String nickname, YearMonth month) {
        Objects.requireNonNull (month);
        Volunteer volunteer = loadVolunteer(nickname);
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findByMonth (month).orElse (null);
        if (plan == null) {return List.of();}

        Map<String, VisitType> visitTypes = visitTypeRepository.findAll ().stream ()
                .filter (Objects::nonNull)
                .collect(Collectors.toMap(VisitType::getId, visit -> visit, (left, right) -> left));
        return plan.getPlannedVisits ().stream ()
                .filter (Objects::nonNull)
                .filter (visit -> visit.getStatus () == VisitStatus.CONFIRMED)
                .filter (visit -> visit.getAssignedVolunteerIds ().contains (volunteer.getNickname ()))
                .map (visit -> DTOMapper.toVisitOccurrenceDTO (plan, visit, visitTypes.get (visit.getVisitTypeId ()), true))
                .filter (Objects::nonNull)
                .sorted (Comparator.comparing (VisitOccurrenceDTO::getDate)
                        .thenComparing (VisitOccurrenceDTO::getStartTime, Comparator.nullsLast (Comparator.naturalOrder ())))
                .toList ();
    }

    private Volunteer loadVolunteer(String nickname){
        String sanitized = requireNonBlank(nickname, "Il nickname del volontario non può essere vuoto");
        return volunteerRepository.findByNickname (sanitized)
                .orElseThrow (() -> new IllegalArgumentException ("Volontario non trovato: " + sanitized));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        return credentialManager.isFirstAccessPending(nickname);
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        credentialManager.verifyDefaultCredentials(nickname, password);
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        credentialManager.setPersonalCredentials(currentNickname, newNickname, password);
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        return credentialManager.verifyLogin(nickname, password);
    }

    private void ensureAvailabilityNotOnBlackoutDates(MonthlyAvailability availability) {
        settingsRepository.load()
                .map(SystemSettings::getExcludedDates)
                .ifPresent(dates -> {
                    if (dates.isEmpty()) {
                        return;
                    }

                    YearMonth referenceMonth = availability.getReferenceMonth();
                    List<LocalDate> conflicts = dates.stream()
                            .filter(Objects::nonNull)
                            .filter(date -> YearMonth.from(date).equals(referenceMonth))
                            .filter(date -> availability.getPreferredDays().contains(date.getDayOfWeek()))
                            .sorted()
                            .toList();

                    if (!conflicts.isEmpty()) {
                        String formatted = conflicts.stream()
                                .map(LocalDate::toString)
                                .reduce((left, right) -> left + ", " + right)
                                .orElse("");
                        throw new IllegalArgumentException(
                                "Le date %s sono precluse alle visite per il mese %s."
                                        .formatted(formatted, referenceMonth)
                        );
                    }
                });
    }

    private void ensureSubmissionAllowed(YearMonth referenceMonth) {
        settingsRepository.load().ifPresent(settings -> {
            YearMonth activePlanningMonth = settings.getActivePlanningMonth();
            PlanningPhase phase = settings.getPlanningPhase ();
            if (activePlanningMonth == null || phase == null) {return;}
            if (activePlanningMonth.equals(referenceMonth) && phase != PlanningPhase.AVAILABILITY_COLLECTION_OPEN) {
                throw new IllegalStateException (
                        "La finestra di caricamento per il mese " + referenceMonth + " è stata chiusa dal configuratore");
            }
        });
    }
}
