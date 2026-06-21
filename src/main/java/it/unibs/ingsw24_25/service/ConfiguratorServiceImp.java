package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.DTO.response.MonthlyPlanResponseDTO;
import it.unibs.ingsw24_25.DTO.response.PlaceResponse;
import it.unibs.ingsw24_25.DTO.response.VisitTypeResponse;
import it.unibs.ingsw24_25.DTO.response.VolunteerResponse;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.DTOMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConfiguratorServiceImp implements ConfiguratorService {

    private final PlaceRepository placeRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final VolunteerRepository volunteerRepository;
    private final SettingsRepository settingsRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final PlannedVisitRepository plannedVisitRepository;
    private final ConfiguratorCredentialManager credentialManager;

    public ConfiguratorServiceImp(PlaceRepository placeRepository,
                                  VisitTypeRepository visitTypeRepository,
                                  VolunteerRepository volunteerRepository,
                                  SettingsRepository settingsRepository,
                                  MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                  PlannedVisitRepository plannedVisitRepository,
                                  ConfiguratorCredentialManager credentialManager) {
        this.placeRepository = Objects.requireNonNull(placeRepository);
        this.visitTypeRepository = Objects.requireNonNull(visitTypeRepository);
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.monthlyVisitPlanRepository = Objects.requireNonNull(monthlyVisitPlanRepository);
        this.plannedVisitRepository = Objects.requireNonNull(plannedVisitRepository);
        this.credentialManager = Objects.requireNonNull(credentialManager);
    }

    /**
     * Genera le visite pianificate per un piano mensile applicando tre vincoli:
     * 1. Anti-overlap: due tipi di visita nello stesso luogo non possono occupare la stessa fascia oraria.
     * 2. WeeklyFrequency: ogni volontario non viene assegnato oltre il proprio limite di turni settimanali.
     * 3. ValidFrom/ValidTo: i tipi di visita vengono inclusi solo nel loro periodo di validità.
     */
    @Override
    @Transactional
    public void generateMonthlyPlanVisits(Long planId) {
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Monthly plan not found"));

        YearMonth targetMonth = plan.getTargetMonth();
        Set<LocalDate> excludedDates = plan.getExcludedDates();
        List<Volunteer> volunteers = volunteerRepository.findAll();
        List<VisitType> visitTypes = visitTypeRepository.findAll();

        // ALTO #2: track weekly shift count per volunteer (volunteerId → weekNumber → count)
        Map<Long, Map<Integer, Integer>> weeklyShiftCount = new HashMap<>();

        List<PlannedVisit> generatedVisits = new ArrayList<>();

        for (LocalDate date = targetMonth.atDay(1);
             date.isBefore(targetMonth.atEndOfMonth().plusDays(1));
             date = date.plusDays(1)) {

            if (excludedDates.contains(date)) continue;

            // Capture loop variable as final for use in lambdas
            final LocalDate currentDate = date;
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            int weekNumber = currentDate.get(WeekFields.ISO.weekOfWeekBasedYear());

            List<Volunteer> availableVolunteers = volunteers.stream()
                    .filter(v -> {
                        Optional<MonthlyAvailability> avail = v.findAvailability(targetMonth);
                        return avail.isPresent() && avail.get().getPreferredDays().contains(dayOfWeek);
                    })
                    .collect(Collectors.toList());

            if (availableVolunteers.isEmpty()) continue;

            for (VisitType visitType : visitTypes) {
                // ALTO #3: skip if visit type not valid for this date
                if (visitType.getValidFrom() != null && currentDate.isBefore(visitType.getValidFrom())) continue;
                if (visitType.getValidTo() != null && currentDate.isAfter(visitType.getValidTo())) continue;

                for (TimeSlot schedule : visitType.getSchedules()) {
                    if (schedule.getDay() != dayOfWeek) continue;

                    // ALTO #1: check time overlap with already-scheduled visits at the same place on the same date
                    LocalTime slotStart = schedule.getStartTime();
                    LocalTime slotEnd = slotStart.plusMinutes(schedule.getDuration());
                    String placeId = visitType.getPlace() != null ? visitType.getPlace().getId() : null;

                    boolean hasConflict = placeId != null && generatedVisits.stream()
                            .filter(pv -> pv.getVisitDate().equals(currentDate))
                            .filter(pv -> pv.getVisitType().getPlace() != null
                                    && pv.getVisitType().getPlace().getId().equals(placeId))
                            .anyMatch(pv -> {
                                for (TimeSlot existing : pv.getVisitType().getSchedules()) {
                                    if (existing.getDay() == dayOfWeek) {
                                        LocalTime existingStart = existing.getStartTime();
                                        LocalTime existingEnd = existingStart.plusMinutes(existing.getDuration());
                                        // Intervals overlap when NOT (slotEnd <= existingStart OR existingEnd <= slotStart)
                                        if (!(slotEnd.compareTo(existingStart) <= 0
                                                || existingEnd.compareTo(slotStart) <= 0)) {
                                            return true;
                                        }
                                    }
                                }
                                return false;
                            });

                    if (hasConflict) continue;

                    // ALTO #2: find volunteer respecting weeklyFrequency limit
                    Optional<Volunteer> assignedVolunteer = availableVolunteers.stream()
                            .filter(v -> v.getVisitsAttending().contains(visitType))
                            .filter(v -> {
                                Optional<MonthlyAvailability> avail = v.findAvailability(targetMonth);
                                if (avail.isEmpty()) return false;
                                int maxWeekly = avail.get().getWeeklyFrequency();
                                if (maxWeekly <= 0) return true;
                                int assigned = weeklyShiftCount
                                        .getOrDefault(v.getId(), Collections.emptyMap())
                                        .getOrDefault(weekNumber, 0);
                                return assigned < maxWeekly;
                            })
                            .findFirst();

                    if (assignedVolunteer.isPresent()) {
                        Volunteer volunteer = assignedVolunteer.get();
                        generatedVisits.add(new PlannedVisit(plan, visitType, volunteer, currentDate, schedule.getStartTime()));
                        availableVolunteers.remove(volunteer);

                        // ALTO #2: update weekly shift counter
                        weeklyShiftCount
                                .computeIfAbsent(volunteer.getId(), k -> new HashMap<>())
                                .merge(weekNumber, 1, Integer::sum);
                    }
                }
            }
        }

        plan.setVisits(generatedVisits);
        plan.setPhase(PlanningPhase.PIANIFICAZIONE_COMPLETATA);
        monthlyVisitPlanRepository.save(plan);
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
        credentialManager.setPersonalCredentials(
                currentNickname != null ? currentNickname.trim() : null,
                newNickname != null ? newNickname.trim() : null,
                password != null ? password.trim() : null
        );
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        return credentialManager.verifyLogin(
                nickname != null ? nickname.trim() : null,
                password != null ? password.trim() : null
        );
    }

    @Override
    @Transactional
    public SystemSettingsDTO getSystemSettings() {
        return settingsRepository.findById(1L)
                .map(settings -> new SystemSettingsDTO(settings.getTerritorialScope(), settings.getMaxPeoplePerSubscription(), settings.getExcludedDates()))
                .orElse(null);
    }

    @Override
    @Transactional
    public void defineTerritorialScope(String scope) {
        try {
            SystemSettings settings = settingsRepository.findById(1L).orElse(new SystemSettings(1L, null, 15, List.of(), null, PlanningPhase.RACCOLTA_DISPONIBILITA, null));
            if (settings.getTerritorialScope() != null) {
                throw new IllegalStateException("Territorial scope already defined as " + settings.getTerritorialScope() + " and cannot be modified");
            }
            settings.setTerritorialScope(scope);
            settingsRepository.save(settings);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Settings were updated by another user. Please try again.", e);
        }
    }

    /**
     * Imposta l'ambito territoriale per il sistema.
     * Questa operazione inizializza le impostazioni di sistema se non esistono.
     * L'ambito territoriale può essere impostato una sola volta.
     *
     * @param scope L'ambito territoriale da impostare (es. nome di una città).
     * @throws IllegalStateException se l'ambito territoriale è già stato definito o in caso di accesso concorrente.
     */
    @Override
    @Transactional
    public void setTerritorialScope(String scope) {
        try {
            SystemSettings settings = settingsRepository.findById(1L).orElse(new SystemSettings(1L, scope, 15, List.of(), null, PlanningPhase.RACCOLTA_DISPONIBILITA, null));
            if (settings.getTerritorialScope() != null && !settings.getTerritorialScope().equals(scope)) {
                throw new IllegalStateException("Territorial scope already defined as " + settings.getTerritorialScope() + " and cannot be modified");
            }
            settings.setTerritorialScope(scope);
            settingsRepository.save(settings);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Settings were updated by another user. Please try again.", e);
        }
    }

    /**
     * Imposta il numero massimo di persone che un beneficiario può inserire in una singola prenotazione.
     *
     * @param max Il numero massimo consentito (deve essere maggiore di 0).
     * @throws IllegalArgumentException se il valore passato è minore di 1.
     * @throws IllegalStateException se le impostazioni di sistema non sono state prima inizializzate con l'ambito territoriale.
     */
    @Override
    @Transactional
    public void setMaxPeoplePerSubscription(int max) {
        try {
            if (max < 1) throw new IllegalArgumentException("Max people per subscription must be positive");
            SystemSettings settings = settingsRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("SystemSettings not yet initialized: please set the territorial scope first"));
            settings.setMaxPeoplePerSubscription(max);
            settingsRepository.save(settings);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Settings were updated by another user. Please try again.", e);
        }
    }

    /**
     * Aggiunge un nuovo luogo (es. museo, parco) al catalogo.
     *
     * @param name Il nome del luogo (deve essere univoco).
     * @param description Una descrizione testuale del luogo.
     * @param location L'indirizzo fisico o la località.
     * @return Una stringa di conferma dell'avvenuto inserimento.
     * @throws IllegalArgumentException se esiste già un luogo con lo stesso nome.
     */
    @Override
    @Transactional
    public String addPlace(String name, String description, String location) {
        if (placeRepository.findByPlaceTitle(name).isPresent()) {
            throw new IllegalArgumentException("A place with this name already exists");
        }
        Place place = new Place(name, description, location);
        placeRepository.save(place);
        return "Place inserted: " + name;
    }

    /**
     * Aggiunge un nuovo tipo di visita guidata, associandola a un luogo esistente.
     *
     * @param placeID L'ID del luogo in cui si svolge la visita.
     * @param title Il titolo della visita (deve essere univoco tra le visite pianificate).
     * @param description Descrizione della visita.
     * @param meetLocation Punto di ritrovo.
     * @param schedules Lista degli orari in cui la visita viene solitamente proposta.
     * @param ticketRequired Indica se è necessario l'acquisto di un biglietto.
     * @param minParticipants Numero minimo di partecipanti affinché la visita sia confermata.
     * @param maxParticipants Numero massimo di partecipanti ammessi.
     * @param validFrom Data di inizio validità del tipo di visita.
     * @param validTo Data di fine validità del tipo di visita.
     * @return Stringa di conferma con ID generato.
     * @throws EntityNotFoundException se il luogo specificato non esiste.
     * @throws IllegalArgumentException se esiste già una visita pianificata con lo stesso titolo.
     */
    @Override
    @Transactional
    public String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules, boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo) {
        Place place = placeRepository.findById(placeID).orElseThrow(() -> new EntityNotFoundException("Place not found"));
        if (visitTypeRepository.findAll().stream().anyMatch(vt -> vt.getVisitTitle().equals(title))) {
            throw new IllegalArgumentException("A visit with this title already exists");
        }
        VisitType visitType = new VisitType(title, description, meetLocation, validFrom, validTo, schedules, ticketRequired, minParticipants, maxParticipants, place);
        visitTypeRepository.save(visitType);
        return "Visit inserted: %s (ID: %s)".formatted(title, visitType.getId());
    }

    /**
     * Registra un nuovo account volontario nel sistema.
     *
     * @param nickname Il nickname desiderato per il volontario.
     * @param defaultPassword La password provvisoria per il primo accesso.
     * @throws IllegalArgumentException se il nickname è già in uso.
     */
    @Override
    @Transactional
    public void addVolunteer(String nickname, String defaultPassword) {
        if (volunteerRepository.findByNickname(nickname).isPresent()) {
            throw new IllegalArgumentException("Nickname already exists");
        }
        Volunteer volunteer = new Volunteer(nickname, defaultPassword);
        volunteerRepository.save(volunteer);
    }

    /**
     * Abilita un volontario a poter fare da guida per un determinato tipo di visita.
     *
     * @param nickname Il nickname del volontario.
     * @param visitTypeId L'ID del tipo di visita.
     * @throws EntityNotFoundException se il volontario o la visita non esistono.
     */
    @Override
    @Transactional
    public void linkVolunteerToVisit(String nickname, String visitTypeId) {
        Volunteer volunteer = volunteerRepository.findByNickname(nickname)
                .orElseThrow(() -> new EntityNotFoundException("Volunteer not found"));
        VisitType visitType = visitTypeRepository.findById(visitTypeId).orElseThrow(() -> new EntityNotFoundException("Visit not found"));
        volunteer.addVisit(visitType);
        volunteerRepository.save(volunteer);
    }

    @Override
    public List<PlaceResponse> listPlace() {
        return placeRepository.findAll().stream()
                .map(place -> new PlaceResponse(place.getId(), place.getPlaceTitle(), place.getPlaceDescription(), place.getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitTypeResponse> listVisitTypeByPlace(String placeId) {
        if (!placeRepository.existsById(placeId)) {
            throw new EntityNotFoundException("No place found with id %s".formatted(placeId));
        }
        return visitTypeRepository.findAll().stream()
                .filter(vt -> vt.getPlace().getId().equals(placeId))
                .map(vt -> new VisitTypeResponse(vt.getId(), vt.getVisitTitle(), vt.getDescription(), vt.getMeetLocation(), vt.getValidFrom(), vt.getValidTo(), vt.isTicketRequired(), vt.getMinParticipants(), vt.getMaxParticipants(), null))
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitTypeResponse> listVisitType() {
        return visitTypeRepository.findAll().stream()
                .map(vt -> new VisitTypeResponse(vt.getId(), vt.getVisitTitle(), vt.getDescription(), vt.getMeetLocation(), vt.getValidFrom(), vt.getValidTo(), vt.isTicketRequired(), vt.getMinParticipants(), vt.getMaxParticipants(), null))
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitOccurrenceDTO> listPlannedVisitsWithStatus() {
        Map<String, VisitType> visitTypes = visitTypeRepository.findAll().stream()
                .collect(Collectors.toMap(VisitType::getId, visit -> visit));

        return monthlyVisitPlanRepository.findAll().stream()
                .flatMap(plan -> plan.getVisits().stream()
                        .map(visit -> DTOMapper.toVisitOccurrenceDTO(plan, visit, visitTypes.get(visit.getVisitType().getId()), false)))
                .sorted(Comparator.comparing(VisitOccurrenceDTO::getDate)
                        .thenComparing(VisitOccurrenceDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VisitOccurrenceDTO::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    public List<VolunteerResponse> listVolunteerWVisitType() {
        return volunteerRepository.findAll().stream()
                .map(DTOMapper::volunteerToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Imposta le date in cui il servizio è sospeso (es. festività nazionali).
     * In questi giorni non verranno generate o pianificate visite.
     *
     * @param dates Lista delle date di chiusura.
     * @throws IllegalStateException se le impostazioni non sono state inizializzate o in caso di accesso concorrente.
     */
    @Override
    @Transactional
    public void setBlackoutDates(List<LocalDate> dates) {
        try {
            SystemSettings settings = settingsRepository.findById(1L)
                    .orElseThrow(() -> new IllegalStateException("System settings not yet initialized"));
            List<LocalDate> sanitizedDates = dates == null ? List.of() : dates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            settings.setExcludedDates(sanitizedDates);
            settingsRepository.save(settings);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Settings were updated by another user. Please try again.", e);
        }
    }

    /**
     * Chiude la finestra temporale per la raccolta delle disponibilità dei volontari.
     * Salva uno snapshot (fotografia) delle disponibilità attuali per procedere alla generazione del piano.
     *
     * @param today Data odierna utilizzata per marcare la chiusura.
     * @throws IllegalStateException se la finestra è già chiusa o il ciclo è in una fase avanzata.
     */
    @Override
    @Transactional
    public void closeAvailabilityWindow(LocalDate today) {
        try {
            SystemSettings settings = requireInitializedSettings();
            if (settings.getPlanningPhase() != PlanningPhase.RACCOLTA_DISPONIBILITA) {
                throw new IllegalStateException("Availability window is already closed or the cycle is in a later phase");
            }

            YearMonth effectiveTargetMonth = settings.getActivePlanningMonth();
            if (effectiveTargetMonth == null) {
                effectiveTargetMonth = YearMonth.now();
            }
            final YearMonth targetMonth = effectiveTargetMonth;

            MonthlyVisitPlan plan = monthlyVisitPlanRepository.findByTargetMonth(targetMonth)
                    .orElseGet(() -> new MonthlyVisitPlan(targetMonth));

            plan.setPhase(PlanningPhase.PIANIFICAZIONE_COMPLETATA);
            monthlyVisitPlanRepository.save(plan);

            settings.setActivePlanningMonth(targetMonth);
            settings.setPlanningPhase(PlanningPhase.PIANIFICAZIONE_COMPLETATA);
            settings.setLastAvailabilityWindowClosure(today);
            settingsRepository.save(settings);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("The planning process was updated by another user. Please refresh and try again.", e);
        }
    }

    /**
     * Genera la bozza iniziale del piano visite mensile.
     * Espande i tipi di visita attivi nei giorni del mese specificato, tenendo conto delle date escluse.
     *
     * @param targetMonth Il mese per cui generare il piano.
     * @return DTO contenente i dettagli del piano appena generato.
     * @throws IllegalStateException se la finestra di disponibilità non è chiusa.
     * @throws IllegalArgumentException se il mese richiesto non è quello di pianificazione attiva.
     */
    @Override
    @Transactional
    public MonthlyPlanDTO generateMonthlyPlan(YearMonth targetMonth) {
        YearMonth currentMonth = YearMonth.now();
        if (targetMonth.isBefore(currentMonth.plusMonths(1))) {
            throw new IllegalArgumentException("La pianificazione può essere generata solo per i mesi futuri.");
        }
        if (targetMonth.equals(currentMonth.plusMonths(1)) && LocalDate.now().getDayOfMonth() > 15) {
            throw new IllegalStateException("Monthly plan can only be generated on or before the 15th of the month.");
        }
        try {
            SystemSettings settings = requireInitializedSettings();
            if (settings.getPlanningPhase() == PlanningPhase.RACCOLTA_DISPONIBILITA) {
                closeAvailabilityWindow(LocalDate.now());
                settings = requireInitializedSettings(); // Re-fetch settings after phase change
            }
            if (!settings.getActivePlanningMonth().equals(targetMonth)) {
                throw new IllegalArgumentException("Requested month does not match the active month " + settings.getActivePlanningMonth());
            }

            MonthlyVisitPlan plan = requirePlan(targetMonth);
            generateMonthlyPlanVisits(plan.getId());
            return DTOMapper.planToDTO(plan, visitTypeRepository.findAll());
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("The planning process was updated by another user. Please refresh and try again.", e);
        }
    }

    @Override
    public List<MonthlyPlanResponseDTO> listMonthlyPlans() {
        return monthlyVisitPlanRepository.findAll().stream()
                .map(plan -> new MonthlyPlanResponseDTO(plan.getId(), plan.getTargetMonth(), plan.getPhase()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void publishMonthlyPlan(Long planId) {
        updatePlanPhase(planId, PlanningPhase.PUBBLICATO);
    }

    @Override
    @Transactional
    public void updatePlanPhase(Long planId, PlanningPhase newPhase) {
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Piano mensile non trovato"));
        plan.setPhase(newPhase); // This will throw IllegalStateException if transition is invalid
        monthlyVisitPlanRepository.save(plan);
    }

    /**
     * Assegna manualmente un volontario a fungere da guida per una visita specifica nel piano mensile.
     * Verifica i vincoli di abilitazione e disponibilità del volontario.
     *
     * @param month Il mese in cui cade la visita.
     * @param date La data esatta della visita.
     * @param slot L'orario di inizio e durata della visita.
     * @param visitTypeId Identificativo del tipo di visita.
     * @param volunteerNickname Nickname del volontario da assegnare.
     * @throws IllegalStateException se il piano non è ancora generato, il volontario non è attivo o non disponibile.
     * @throws IllegalArgumentException se il volontario non è abilitato a quel tipo di visita.
     */
    @Override
    @Transactional
    public void assignVolunteerToPlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId, String volunteerNickname) {
        try {
            SystemSettings settings = requireInitializedSettings();
            if (settings.getPlanningPhase() != PlanningPhase.PIANIFICAZIONE_COMPLETATA) {
                throw new IllegalStateException("Assignments are only allowed after the plan has been generated");
            }
            if (!settings.getActivePlanningMonth().equals(month)) {
                throw new IllegalArgumentException("Indicated month does not match the active month " + settings.getActivePlanningMonth());
            }

            MonthlyVisitPlan plan = requirePlan(month);
            PlannedVisit plannedVisit = findPlannedVisit(plan, date, slot.getStartTime(), visitTypeId);

            Volunteer volunteer = volunteerRepository.findByNickname(volunteerNickname)
                    .orElseThrow(() -> new EntityNotFoundException("Volunteer not found: " + volunteerNickname));
            if (!volunteer.isActive()) {
                throw new IllegalStateException("Volunteer " + volunteerNickname + " is not active");
            }

            if (volunteer.getVisitsAttending().stream().noneMatch(visit -> visit.getId().equals(visitTypeId))) {
                throw new IllegalArgumentException("Volunteer " + volunteerNickname + " is not enabled for visit " + visitTypeId);
            }

            plannedVisit.setVolunteer(volunteer);
            monthlyVisitPlanRepository.save(plan);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("The planning process was updated by another user. Please refresh and try again.", e);
        }
    }

    /**
     * Rimuove una visita pianificata dal piano mensile e gestisce l'eventuale rimozione
     * dei turni assegnati ai volontari per quella specifica occorrenza.
     *
     * @param month Il mese del piano.
     * @param date La data della visita.
     * @param slot Fascia oraria.
     * @param visitTypeId L'ID del tipo di visita.
     * @throws IllegalStateException se il piano non è stato ancora generato.
     */
    @Override
    @Transactional
    public void removePlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId) {
        try {
            SystemSettings settings = requireInitializedSettings();
            if (settings.getPlanningPhase().ordinal() < PlanningPhase.PIANIFICAZIONE_COMPLETATA.ordinal()) {
                throw new IllegalStateException("Visits can only be removed after the plan has been generated");
            }

            MonthlyVisitPlan plan = requirePlan(month);
            PlannedVisit plannedVisit = findPlannedVisit(plan, date, slot.getStartTime(), visitTypeId);

            plan.getVisits().remove(plannedVisit);
            monthlyVisitPlanRepository.save(plan);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("The planning process was updated by another user. Please refresh and try again.", e);
        }
    }

    /**
     * Rimuove fisicamente un luogo dal sistema e, in cascata, cancella tutti
     * i tipi di visita associati ad esso.
     *
     * @param placeId ID del luogo da eliminare.
     * @throws EntityNotFoundException se il luogo non esiste.
     */
    @Override
    @Transactional
    public void removePlace(String placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new EntityNotFoundException("Place not found: " + placeId));

        // Clean volunteer associations for each visit type before cascading delete
        for (VisitType vt : new ArrayList<>(place.getVisits())) {
            String vtId = vt.getId();
            for (Volunteer volunteer : volunteerRepository.findAll()) {
                if (volunteer.getVisitsAttending().removeIf(v -> v.getId().equals(vtId))) {
                    volunteerRepository.save(volunteer);
                }
            }
        }

        // Delete place → cascades to VisitTypes (cascade=ALL) → PlannedVisits (cascade=REMOVE)
        placeRepository.delete(place);
    }

    /**
     * Rimuove un tipo di visita. Elimina le associazioni con i volontari.
     * Se l'eliminazione lascia un luogo senza visite o un volontario senza visite,
     * vengono rimossi a loro volta per mantenere la consistenza del sistema.
     *
     * @param visitTypeId L'ID della visita da cancellare.
     * @throws EntityNotFoundException se la visita non esiste.
     */
    @Override
    @Transactional
    public void removeVisitType(String visitTypeId) {
        VisitType visitType = visitTypeRepository.findById(visitTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Visit not found: " + visitTypeId));

        for (Volunteer volunteer : volunteerRepository.findAll()) {
            if (volunteer.getVisitsAttending().removeIf(vt -> vt.getId().equals(visitTypeId))) {
                if (volunteer.getVisitsAttending().isEmpty()) {
                    removeVolunteer(volunteer.getNickname());
                } else {
                    volunteerRepository.save(volunteer);
                }
            }
        }

        Place place = visitType.getPlace();
        if (place != null) {
            place.getVisits().remove(visitType);
            if (place.getVisits().isEmpty()) {
                placeRepository.delete(place);
            } else {
                placeRepository.save(place);
            }
        }

        visitTypeRepository.delete(visitType);
    }

    /**
     * Rimuove permanentemente un account volontario dal sistema.
     * Promuove la fase di pianificazione a REVIEW se necessario.
     *
     * @param nickname Nickname del volontario.
     * @throws EntityNotFoundException se il volontario non esiste.
     */
    @Override
    @Transactional
    public void removeVolunteer(String nickname) {
        Volunteer volunteer = volunteerRepository.findByNickname(nickname)
                .orElseThrow(() -> new EntityNotFoundException("Volunteer not found: " + nickname));

        // Null out volunteer_id in PlannedVisits to avoid FK constraint violation
        List<PlannedVisit> assignedVisits = plannedVisitRepository.findByVolunteer(volunteer);
        for (PlannedVisit pv : assignedVisits) {
            pv.setVolunteer(null);
        }
        plannedVisitRepository.saveAll(assignedVisits);

        volunteerRepository.delete(volunteer);
    }

    /**
     * Chiude il ciclo di pianificazione del mese corrente e riapre la finestra
     * di acquisizione disponibilità per il mese successivo.
     * Pulisce gli snapshot e le disponibilità del mese appena elaborato.
     *
     * @param today La data di esecuzione dell'operazione.
     * @throws IllegalStateException se la fase corrente non permette la riapertura.
     */
    @Override
    @Transactional
    public void reopenAvailabilityWindow(LocalDate today) {
        try {
            SystemSettings settings = requireInitializedSettings();
            if (settings.getPlanningPhase().ordinal() < PlanningPhase.PIANIFICAZIONE_COMPLETATA.ordinal()) {
                throw new IllegalStateException("Window can only be reopened after the assignment phase");
            }

            YearMonth completedMonth = settings.getActivePlanningMonth();
            if (completedMonth != null) {
                monthlyVisitPlanRepository.findByTargetMonth(completedMonth).ifPresent(plan -> {
                    plan.setPhase(PlanningPhase.RACCOLTA_DISPONIBILITA);
                    monthlyVisitPlanRepository.save(plan);
                });
                volunteerRepository.findAll().forEach(volunteer -> {
                    volunteer.removeAvailability(completedMonth);
                    volunteerRepository.save(volunteer);
                });
            }

            YearMonth nextMonth = (completedMonth == null) ? YearMonth.now() : completedMonth.plusMonths(1);
            settings.setActivePlanningMonth(nextMonth);
            settings.setPlanningPhase(PlanningPhase.RACCOLTA_DISPONIBILITA);
            settingsRepository.save(settings);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("The planning process was updated by another user. Please refresh and try again.", e);
        }
    }

    @Override
    public MonthlyPlanDTO getMonthlyPlanDetails(YearMonth month) {
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findByTargetMonth(month)
                .orElseThrow(() -> new IllegalStateException("No plan available for " + month));
        return DTOMapper.planToDTO(plan, visitTypeRepository.findAll());
    }

    @Override
    public List<String> listConfigurators() {
        return credentialManager.listConfigurators();
    }

    @Override
    public boolean hasPendingConfiguratorSeeds() {
        return credentialManager.hasPendingConfiguratorSeeds();
    }

    @Override
    @Transactional
    public void registerConfigurator(String nickname, String password) {
        credentialManager.registerConfigurator(nickname, password);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        credentialManager.changePassword(username, oldPassword, newPassword);
    }

    private SystemSettings requireInitializedSettings() {
        return settingsRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("System settings not yet initialized"));
    }

    private MonthlyVisitPlan requirePlan(YearMonth month) {
        return monthlyVisitPlanRepository.findByTargetMonth(month)
                .orElseThrow(() -> new IllegalStateException("No monthly plan generated for " + month));
    }

    private PlannedVisit findPlannedVisit(MonthlyVisitPlan plan, LocalDate date, LocalTime time, String visitTypeId) {
        return plan.getVisits().stream()
                .filter(visit -> visit.getVisitType().getId().equals(visitTypeId)
                        && visit.getVisitDate().equals(date)
                        && Objects.equals(visit.getVisitTime(), time))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Planned visit not found"));
    }
}
