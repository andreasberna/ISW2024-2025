package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.MonthlyVisitPlanRepository;
import it.unibs.ingsw24_25.repository.SettingsRepository;
import it.unibs.ingsw24_25.repository.VisitTypeRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
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
    private final Set<String> defaultCredentialsValidated = new HashSet<> ();

    public VolunteerServiceImp(VolunteerRepository volunteerRepository,
                               SettingsRepository settingsRepository,
                               MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                               VisitTypeRepository visitTypeRepository) {
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.monthlyVisitPlanRepository = Objects.requireNonNull(monthlyVisitPlanRepository);
        this.visitTypeRepository = Objects.requireNonNull(visitTypeRepository);
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
        Volunteer volunteer = loadVolunteer(nickname);
        volunteer.deactivate ();
        volunteerRepository.deleteByNickname (volunteer.getNickname ());
        defaultCredentialsValidated.remove(volunteer.getNickname ());
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
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.isFirstAccessPending ();
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        Volunteer volunteer = loadVolunteer (nickname);
        if (!volunteer.isFirstAccessPending ())
            throw new IllegalStateException ("Le credenziali personali sono già state impostate");
        String sanitizedPassword = requireNonBlank(password, "la Password di default non può essere nulla");
        if (!volunteer.passwordMatches (sanitizedPassword))
            throw new IllegalArgumentException ("Credenziali di primo accesso non valide");

        defaultCredentialsValidated.add(volunteer.getNickname ());
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        Volunteer volunteer = loadVolunteer(currentNickname);
        if (!volunteer.isFirstAccessPending ())
            throw new IllegalStateException ("Le credenziali personali sono state impostate");

        String sanitizedPassword = requireNonBlank(password, "La nuova password non può essere nulla");
        String sanitizedNickname = requireNonBlank(newNickname, "Il nuovo nickname non può essere vuoto");

        String current = volunteer.getNickname();
        if (current.equalsIgnoreCase(sanitizedNickname))
            throw new IllegalArgumentException("Il nuovo nickname deve essere diverso da quello assegnato");

        if (volunteer.passwordMatches(sanitizedPassword))
            throw new IllegalArgumentException("La nuova password deve essere diversa da quella assegnata");

        if (!current.equalsIgnoreCase(sanitizedNickname) && volunteerRepository.findByNickname(sanitizedNickname).isPresent())
            throw new IllegalArgumentException("Nickname già presente");

        if(!defaultCredentialsValidated.remove(current))
            throw new IllegalStateException ("Credenziali di default non ancora verificate");

        volunteerRepository.deleteByNickname(current);
        volunteer.setNickname(sanitizedNickname);
        volunteer.setPersonalCredentials (sanitizedPassword);
        volunteerRepository.save(volunteer);
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) return false;
        String sanitizedNick = nickname.trim();
        String sanitizedPassword = password.trim();
        if(sanitizedNick.isEmpty() || sanitizedPassword.isEmpty()) return false;

        Optional<Volunteer> volunteer = volunteerRepository.findByNickname (sanitizedNick);
        if (volunteer.isEmpty ()) return false;

        Volunteer loaded = volunteer.get();
        if (loaded.isFirstAccessPending ()) return false;

        return loaded.passwordMatches (sanitizedPassword);
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
