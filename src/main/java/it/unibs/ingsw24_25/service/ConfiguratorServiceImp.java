package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.MonthlyPlanDTO;
import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.AvailabilitySubmissionPolicy;
import it.unibs.ingsw24_25.util.DTOMapper;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;
import it.unibs.ingsw24_25.util.PlanningWindowPolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ConfiguratorServiceImp implements ConfiguratorService {

    public static final String DEFAULT_NICKNAME = "config";
    public static final String DEFAULT_PASSWORD = "psswrd";

    private final PlaceRepository placeRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final VolunteerRepository volunteerRepository;
    private final SettingsRepository settingsRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final ConfiguratorRepository configuratorRepository;
    private final VolunteerService volunteerService;
    private boolean defaultCredentialsValidated = false;

    public ConfiguratorServiceImp(PlaceRepository placeRepository,
                                  VisitTypeRepository visitTypeRepository,
                                  VolunteerRepository volunteerRepository, SettingsRepository settingsRepository,
                                  MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                  ConfiguratorRepository configuratorRepository,
                                  VolunteerService volunteerService) {
        this.placeRepository = placeRepository;
        this.visitTypeRepository = visitTypeRepository;
        this.volunteerRepository = volunteerRepository;
        this.settingsRepository = settingsRepository;
        this.monthlyVisitPlanRepository = monthlyVisitPlanRepository;
        this.configuratorRepository = configuratorRepository;
        this.volunteerService = volunteerService;
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        return !configuratorRepository.exists ();
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        if(!isFirstAccessPending(nickname))
            throw new IllegalStateException("Le credenziali personali sono già state impostate");
        if(!DEFAULT_NICKNAME.equals(nickname) || !DEFAULT_PASSWORD.equals(password))
            throw new IllegalArgumentException ("Credenziali di primo accesso non valide");

        defaultCredentialsValidated = true;
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        if (!isFirstAccessPending(currentNickname)) throw new IllegalStateException ("Le credenziali sono già state configurate");
        if (!defaultCredentialsValidated) throw new IllegalStateException ("Credenziali di default non ancora verificate");

        String sanitizedNickname = requireNonBlank(newNickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");
        configuratorRepository.save (new Configurator (sanitizedNickname, sanitizedPassword));
        defaultCredentialsValidated = false;
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) return false;
        if (isFirstAccessPending(nickname)) return false;

        String normalizedNickname= nickname.trim ();
        String normalizedPassword = password.trim ();

        if (normalizedNickname.isEmpty() || normalizedPassword.isEmpty())
            return false;

        return configuratorRepository.load ()
                .map (map -> map.get(normalizedNickname))
                .map (configurator ->
                        Objects.equals (configurator.getPassword (), normalizedPassword))
                .orElse(false);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public void setTerritorialScope(String scope) {
        String s = Objects.requireNonNull (scope, "scope nullo");
        if(s.isEmpty ()) throw new IllegalArgumentException ("scope vuoto");

        var current = settingsRepository.load ();
        if (current.isPresent ()){
            if (!current.get ().getTerritorialScope ().equals (s)) {
               throw new IllegalStateException (
                       "Territorial scope già definito come " + current.get ().getTerritorialScope () + "e non è modificabile");
            }
            return;
        }

        int DEFAULT_MAXPARTICIPANTS = 15;

        settingsRepository.save(new SystemSettings(scope, DEFAULT_MAXPARTICIPANTS, List.of ()));
    }

    @Override
    public void setMaxPeoplePerSubscription(int max) {
        if(max < 1)  throw new IllegalArgumentException ("max people per subscription deve essere positiva");

        var current = settingsRepository.load ()
                .orElseThrow (() -> new IllegalStateException ("SystemSettings non ancora inizializzati: impostare prima l'ambito territoriale"));

        //Idempotenza
        if (current.getMaxPeoplePerSubscription () == max) return;

        current.setMaxPeoplePerSubscription (max);
        settingsRepository.save(current);

    }

    @Override
    public String addPlace(String name, String description, String location) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException ("Il nome del luogo non può essere nullo");

        if (placeRepository.findById (name).isPresent())
            throw new IllegalArgumentException ("Un luogo con questo nome esiste già");

        Place place = new Place(name, description, location);
        place.setVisits (new ArrayList<> ());
        placeRepository.save(place);
        return "Luogo inserito: " + name;
    }

    @Override
    public String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules, boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo) {
        if(placeID == null || placeID.isBlank())
            throw  new IllegalArgumentException ("Identificativo luogo non valido");

        if (title == null || title.isBlank())
            throw new IllegalArgumentException ("Titolo visita non valido");

        if(minParticipants <= 0 || maxParticipants <= 0 || minParticipants > maxParticipants)
            throw new  IllegalArgumentException ("Numero partecipanti non valido");

        ensureCatalogAdditionCompatible(validFrom);

        Place place = placeRepository.findById (placeID)
                .orElseThrow (() -> new IllegalArgumentException ("Luogo non trovato"));
        List<VisitType> existingVisits = Optional.ofNullable(visitTypeRepository.findAll ()).orElse (List.of());
        boolean duplicateTitle = existingVisits.stream ()
                .filter (Objects::nonNull)
                .anyMatch (existing -> existing.getVisitTitle () != null
                        && existing.getVisitTitle ().equalsIgnoreCase (title));
        if (duplicateTitle) {
            throw new IllegalArgumentException ("Esiste già una visita con questo titolo");
        }

        List<TimeSlot> copySchedule = schedules == null ? new ArrayList<>() : new ArrayList<> (schedules);
        VisitType visitType = new VisitType (
                title,
                description,
                meetLocation,
                validFrom,
                validTo,
                copySchedule,
                ticketRequired,
                minParticipants,
                maxParticipants,
                place,
                new ArrayList<> ()
        );
        visitTypeRepository.save(visitType);
        if (place.getVisits () == null) place.setVisits (new ArrayList<> ());
        place.addVisit (visitType);
        placeRepository.save(place);
        return "Visita inserita: %s".formatted(title);
    }

    @Override
    public void addVolunteer(String nickname, String defaultPassword) {
        String sanitizedNick = requireNonBlank (nickname, "nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank (defaultPassword, "password non può essere vuoto");
        if(volunteerRepository.findByNickname (nickname).isPresent()) throw new IllegalArgumentException ("Nickname già presente");
        Volunteer volunteer = new  Volunteer (sanitizedNick, sanitizedPassword);
        volunteer.setVisitsAttending (new ArrayList<> ());
        volunteerRepository.save(volunteer);
    }

    @Override
    public void linkVOlunteerToVisit(String nickname, String visitTypeId) {
        //ricerca in repository del volontario associato al nickname
        Volunteer volunteer = volunteerRepository.findByNickname (nickname)
                .orElseThrow ( () -> new IllegalArgumentException ("Volontario non trovato"));
        //ricerca in repository della visita associata al titolo
        String sanitizedVisitId = requireNonBlank (visitTypeId, "identificativo visita non valido");
        VisitType visitType = resolveVisitTypeByIdentifier(sanitizedVisitId);

        //creazione delle liste per evitare errori in caso di liste non presenti
        if (volunteer.getVisitsAttending () == null) volunteer.setVisitsAttending (new ArrayList<> ());
        if (visitType.getGuides () == null) visitType.setGuides (new ArrayList<> ());

        //link visitType e volunteer
        if (!volunteer.getVisitsAttending ().contains (visitType))
            volunteer.addVisit(visitType);
        if (!visitType.getGuides ().contains (volunteer))
            visitType.addGuide (volunteer);

        //salvataggio in repository dei valori modificati
        volunteerRepository.save(volunteer);
        visitTypeRepository.save(visitType);
    }

    @Override
    public List<PlaceDTO> listPlace() {
        return placeRepository.findAll ().stream ()
                .map (DTOMapper::placeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitTypeDTO> listVisitTypeByPlace(String placeId) {
        if(placeId == null || placeId.isBlank())
            throw new IllegalArgumentException ("Identificativo luogo non valido");

        if (!placeRepository.findById (placeId).isPresent())
            throw new IllegalArgumentException ("Nessun luogo trovato con id %s".formatted(placeId));

        return visitTypeRepository.findByPlace (placeId).stream ()
                .map (DTOMapper::visitTypeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitTypeDTO> listVisitType(){
        return visitTypeRepository.findAll ().stream ()
                .map (DTOMapper::visitTypeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VolunteerDTO> listVolunteerWVisitType() {
        return volunteerRepository.findAll ().stream ()
                .map (DTOMapper::volunteerToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void setBlackoutDates(List<LocalDate> dates) {
        var current = settingsRepository.load ()
                .orElseThrow ( () -> new IllegalStateException ("System settings non ancora inizializzati"));

        List<LocalDate> sanitized = sanitizedExcludedDates(dates);
        ensureAllowedMonth(sanitized);
        current.setExcludedDates (sanitized);
        settingsRepository.save(current);
    }

    private List<LocalDate> sanitizedExcludedDates(List<LocalDate> dates) {
        if (dates == null){
            return List.of();
        }
        return dates.stream()
                .filter(Objects::nonNull)
                .distinct ()
                .sorted ()
                .collect (Collectors.toCollection(ArrayList::new));

    }
    private void ensureAllowedMonth(List<LocalDate> dates) {
        if (dates == null) return;

        YearMonth allowedMonth = ExcludedDatePolicy.allowedMonth(LocalDate.now());
        boolean hasInvalid = dates.stream()
                .anyMatch (date -> !YearMonth.from(date).equals(allowedMonth));
        if (hasInvalid) {
            throw new IllegalArgumentException(
                    "Le date precluse devono appartenere al mese " + allowedMonth + "."
            );
        }
    }

    @Override
    public void closeAvailabilityWindow(LocalDate today){
        Objects.requireNonNull (today, "La data odierna non può essere nulla");
        SystemSettings settings = requireInitializedSettings();
        if (settings.getPlanningPhase () != PlanningPhase.AVAILABILITY_COLLECTION_OPEN)
            throw new IllegalStateException ("La finestra di disponibilità è già stata chiusa o il ciclo è in una fase successiva");

        YearMonth computedMonth = AvailabilitySubmissionPolicy.nextSubmissionMonth (today);
        YearMonth resolvedMonth = settings.getActivePlanningMonth ();
        if (resolvedMonth == null || resolvedMonth.isBefore(computedMonth)) {
            resolvedMonth = computedMonth;
        }
        final YearMonth targetMonth = resolvedMonth;
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findByMonth (resolvedMonth)
                .orElseGet (() -> new MonthlyVisitPlan (targetMonth));
        if (!resolvedMonth.equals (plan.getTargetMonth ()))
            plan = new MonthlyVisitPlan (resolvedMonth);

        plan.setAvailabilitySnapshots (volunteerService.snapshotAvailabilities (resolvedMonth, today));
        plan.replacePlannedVisits (List.of());
        plan.setPhase (PlanningPhase.AVAILABILITY_COLLECTION_CLOSED);
        plan.setAvailabilityWindowClosedOn (today);
        monthlyVisitPlanRepository.save(plan);

        settings.setActivePlanningMonth (resolvedMonth);
        settings.setPlanningPhase (PlanningPhase.AVAILABILITY_COLLECTION_CLOSED);
        settings.setLastAvailabilityWindowClosure (today);
        settingsRepository.save(settings);
    }

    @Override
    public void applyCatalogChange(Runnable change) {
        Objects.requireNonNull (change, "L'operazione di modifica non può essere nulla");
        SystemSettings settings = requireInitializedSettings();
        PlanningPhase phase = settings.getPlanningPhase ();
        if (phase != PlanningPhase.AVAILABILITY_COLLECTION_CLOSED && phase != PlanningPhase.CATALOG_MANAGEMENT)
            throw new IllegalStateException ("Le modifiche al catalogo sono consentite solo dopo la chiusura delle disponibilità");

        YearMonth targetMonth = requireActivePlanningMonth(settings);
        MonthlyVisitPlan plan = requirePlan(targetMonth);
        if (plan.getPhase () == PlanningPhase.AVAILABILITY_COLLECTION_CLOSED) {
            plan.setPhase (PlanningPhase.CATALOG_MANAGEMENT);
            monthlyVisitPlanRepository.save(plan);
        }
        if (settings.getPlanningPhase () != PlanningPhase.CATALOG_MANAGEMENT) {
            settings.setPlanningPhase (PlanningPhase.CATALOG_MANAGEMENT);
            settingsRepository.save(settings);
        }
        change.run ();
    }

    @Override
    public MonthlyPlanDTO generateMonthlyPlan(YearMonth targetMonth) {
        Objects.requireNonNull (targetMonth, "Il mese di pianificazione non può essere nullo");
        SystemSettings settings = requireInitializedSettings();
        if (settings.getPlanningPhase () != PlanningPhase.CATALOG_MANAGEMENT) {
            throw new IllegalStateException ("Il piano può essere generato solo dopo la gestione del catalogo");
        }
        YearMonth activeMonth = requireActivePlanningMonth(settings);
        if (!activeMonth.equals(targetMonth))
            throw new IllegalArgumentException ("Il mese richiesto non corrisponde al mese attivo " + activeMonth);

        MonthlyVisitPlan plan = requirePlan(targetMonth);
        List<PlannedVisit> visits = generatePlannedVisits(targetMonth, settings.getExcludedDates ());
        plan.replacePlannedVisits (visits);
        plan.setPhase (PlanningPhase.PLAN_GENERATED);
        monthlyVisitPlanRepository.save(plan);

        settings.setPlanningPhase (PlanningPhase.PLAN_GENERATED);
        settingsRepository.save(settings);
        return DTOMapper.planToDTO (plan, visitTypeRepository.findAll ());
    }

    @Override
    public void assignVolunteerToPlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId, String volunteerNickname) {
        Objects.requireNonNull(month, "Il mese non può essere nullo");
        Objects.requireNonNull(date, "La data non può essere nulla");
        Objects.requireNonNull(slot, "La fascia oraria non può essere nulla");
        String sanitizedVisitType = requireNonBlank(visitTypeId, "L'identificativo del tipo visita non può essere vuoto");
        String sanitizedVolunteer = requireNonBlank(volunteerNickname, "Il nickname del volontario non può essere vuoto");

        SystemSettings settings = requireInitializedSettings();
        PlanningPhase phase = settings.getPlanningPhase ();
        if( phase != PlanningPhase.PLAN_GENERATED && phase != PlanningPhase.ASSIGNMENT)
            throw new IllegalStateException ("Le assegnazioni sono consentte solo dopo la generazione del piano");

        YearMonth activeMonth = requireActivePlanningMonth(settings);
        if (!activeMonth.equals(month))
            throw new IllegalArgumentException ("Il mese indicato non corrisponde al mese attivo " + activeMonth);

        MonthlyVisitPlan plan = requirePlan(month);
        PlannedVisit plannedVisit = findPlannedVisit(plan, date, slot, sanitizedVisitType);

        Volunteer volunteer = volunteerRepository.findByNickname (sanitizedVolunteer)
                .orElseThrow (() -> new IllegalArgumentException ("Volontario non trovato: " + sanitizedVolunteer));
        if (!volunteer.isActive ())
            throw new IllegalStateException ("Il volontario " + sanitizedVolunteer + " non è attivo");

        boolean attendsVisit = volunteer.getVisitsAttending ().stream ()
                .filter (Objects::nonNull)
                .anyMatch (visit -> sanitizedVisitType.equals (visit.getId ()));
        if (!attendsVisit)
            throw new IllegalArgumentException ("Il volontario " + sanitizedVolunteer + "non è abilitato per la visita " + sanitizedVisitType);

        MonthlyAvailability availability = plan.getAvailabilitySnapshots ().get (sanitizedVolunteer);
        if (availability == null)
            throw new IllegalStateException ("Il volontario " + sanitizedVolunteer + " non ha disponibilità per il mese " + month);
        if(!availability.resolveAvailableDates ().contains (date))
            throw new IllegalStateException ("Il volontario " + sanitizedVolunteer + " non è disponibile in data " + date);

        plannedVisit.assignVolunteer (sanitizedVolunteer);
        plan.setPhase (PlanningPhase.ASSIGNMENT);
        monthlyVisitPlanRepository.save(plan);

        settings.setPlanningPhase (PlanningPhase.ASSIGNMENT);
        settingsRepository.save(settings);

        List<AssignedShift> updatedShifts = new ArrayList<>(volunteer.getShiftsForMonth (month));
        if (updatedShifts.stream ().noneMatch (shift -> matchesShift(shift, date, slot, sanitizedVisitType))){
            updatedShifts.add (new AssignedShift (date, sanitizedVisitType,
                    new TimeSlot (slot.getDay (), slot.getStartTime (), slot.getDuration ())));
            volunteer.assignShifts (month, updatedShifts);
            volunteerRepository.save(volunteer);
        }
    }

    public void removePlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId) {
        Objects.requireNonNull (month, "Il mese non può essere nullo");
        Objects.requireNonNull (date, "La data non può essere nulla");
        Objects.requireNonNull (slot, "La fascia oraria non può essere nulla");
        String sanitizedVisitType = requireNonBlank (visitTypeId, "L'identificativo del tipo visita non può essere vuoto");

        SystemSettings settings = requireInitializedSettings ();
        if (settings.getPlanningPhase ().ordinal () < PlanningPhase.PLAN_GENERATED.ordinal ()) {
            throw new IllegalStateException ("Le visite possono essere rimosse solo dopo la generazione del piano");
        }

        MonthlyVisitPlan plan = requirePlan (month);
        PlannedVisit plannedVisit = findPlannedVisit (plan, date, slot, sanitizedVisitType);

        for (String volunteerId : plannedVisit.getAssignedVolunteerIds ()) {
            volunteerRepository.findByNickname (volunteerId).ifPresent(volunteer -> {
                List<AssignedShift> shifts = new ArrayList<>(volunteer.getShiftsForMonth (month));
                if (shifts.removeIf (shift -> matchesShift(shift, date, slot, sanitizedVisitType))){
                    if (shifts.isEmpty ()) volunteer.removeScheduledShifts (month);
                    else volunteer.assignShifts (month, shifts);
                    volunteerRepository.save(volunteer);
                }
            });
        }

        plan.removePlannedVisit (plannedVisit);
        plan.setPhase (PlanningPhase.REVIEW);
        monthlyVisitPlanRepository.save(plan);

        settings.setPlanningPhase (PlanningPhase.REVIEW);
        settingsRepository.save(settings);
    }

    @Override
    public void removePlace(String placeId) {
        String sanitizedId = requireNonBlank(placeId, "L'identificativo del luogo non può essere vuoto");
        Place place = placeRepository.findById(sanitizedId)
                .orElseThrow(() -> new IllegalArgumentException("Luogo non trovato: " + sanitizedId));

        List<String> visitIds = place.getVisits() == null ? List.of() : place.getVisits().stream()
                .filter(Objects::nonNull)
                .map(VisitType::getId)
                .filter(Objects::nonNull)
                .toList();

        for (String visitId : visitIds) {
            removeVisitType(visitId);
        }

        placeRepository.deleteById(sanitizedId);
    }

    @Override
    public void removeVisitType(String visitTypeId) {
        String sanitizedId = requireNonBlank(visitTypeId, "L'identificativo del tipo visita non può essere vuoto");
        VisitType visitType = resolveVisitTypeByIdentifier(sanitizedId);

        detachVisitFromPlace(visitType);
        purgeVisitFromVolunteers(visitType.getId());
        visitTypeRepository.deleteById(visitType.getId());
        promoteReviewPhaseIfNeeded();
    }

    @Override
    public void removeVolunteer(String nickname) {
        String sanitizedNick = requireNonBlank(nickname, "Il nickname non può essere vuoto");
        Volunteer volunteer = volunteerRepository.findByNickname(sanitizedNick)
                .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + sanitizedNick));

        List<String> visitIds = volunteer.getVisitsAttending().stream()
                .filter(Objects::nonNull)
                .map(VisitType::getId)
                .filter(Objects::nonNull)
                .toList();
        for (String visitId : visitIds) {
            visitTypeRepository.findById(visitId).ifPresent(visit -> {
                visit.getGuides().removeIf(guide -> sanitizedNick.equals(guide.getNickname()));
                visitTypeRepository.save(visit);
            });
        }

        volunteerService.removeVolunteerAccount(sanitizedNick);
        promoteReviewPhaseIfNeeded();
    }

    @Override
    public void reopenAvailabilityWindow(LocalDate today) {
        Objects.requireNonNull(today, "La data odierna non può essere nulla");
        SystemSettings settings = requireInitializedSettings();
        PlanningPhase phase = settings.getPlanningPhase();
        if (phase.ordinal() < PlanningPhase.ASSIGNMENT.ordinal()) {
            throw new IllegalStateException("La finestra può essere riaperta solo dopo la fase di assegnazione");
        }

        YearMonth completedMonth = settings.getActivePlanningMonth();
        if (completedMonth != null) {
            monthlyVisitPlanRepository.findByMonth(completedMonth).ifPresent(plan -> {
                plan.setPhase(PlanningPhase.READY_FOR_NEXT_CYCLE);
                monthlyVisitPlanRepository.save(plan);
            });
        }

        YearMonth desiredMonth = PlanningWindowPolicy.resolveNextPlanningMonth(today);
        YearMonth nextMonth;
        if (completedMonth == null) {
            nextMonth = desiredMonth;
        } else {
            YearMonth sequential = completedMonth.plusMonths(1);
            nextMonth = sequential.isAfter(desiredMonth) ? sequential : desiredMonth;
        }
        settings.setActivePlanningMonth(nextMonth);
        settings.setPlanningPhase(PlanningPhase.AVAILABILITY_COLLECTION_OPEN);
        settingsRepository.save(settings);
    }

    @Override
    public MonthlyPlanDTO getMonthlyPlanDetails(YearMonth month) {
        Objects.requireNonNull(month, "Il mese richiesto non può essere nullo");
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findByMonth(month)
                .orElseThrow(() -> new IllegalStateException("Nessun piano disponibile per " + month));
        return DTOMapper.planToDTO(plan, visitTypeRepository.findAll());
    }

    private SystemSettings requireInitializedSettings() {
        return settingsRepository.load ()
                .orElseThrow(() -> new IllegalStateException("System settings non ancora inizializzati"));
    }

    private YearMonth requireActivePlanningMonth(SystemSettings settings) {
        YearMonth active = settings.getActivePlanningMonth();
        if (active == null) {
            throw new IllegalStateException("Nessun mese di pianificazione attivo");
        }
        return active;
    }

    private MonthlyVisitPlan requirePlan(YearMonth month) {
        return monthlyVisitPlanRepository.findByMonth (month)
                .orElseThrow (() -> new IllegalStateException("Nessun piano mensile generato per " + month));
    }

    private PlannedVisit findPlannedVisit(MonthlyVisitPlan plan, LocalDate date, TimeSlot slot, String visitTypeId) {
        Optional<PlannedVisit> match = plan.getPlannedVisits ().stream()
                .filter (visit -> visit!= null
                                    && visitTypeId.equals (visit.getVisitTypeId ())
                                    && date.equals (visit.getDate())
                                    && sameSlot(visit.getTimeSlot (), slot))
                .findFirst ();
        return match.orElseThrow (() -> new IllegalArgumentException ("Visita pianificata non trovata"));
    }

    private boolean sameSlot(TimeSlot left, TimeSlot right) {
        if (left == null || right == null) {return false;}

        return Objects.equals (left.getDay (), right.getDay ())
                && Objects.equals (left.getStartTime (), right.getStartTime ())
                &&Objects.equals (left.getDuration (), right.getDuration ());
    }

    private boolean matchesShift(AssignedShift shift, LocalDate date, TimeSlot slot, String visitTypeId) {
        if (shift == null) {return false;}

        return visitTypeId.equals (shift.getVisitTypeId ())
                && date.equals (shift.getDate())
                && sameSlot (shift.getSlot(), slot);
    }

    private List<PlannedVisit> generatePlannedVisits(YearMonth targetMonth, List<LocalDate> excludedDates) {
        Set<LocalDate> excluded = excludedDates == null ? Set.of() : new  HashSet<>(excludedDates);
        return visitTypeRepository.findAll().stream()
                .filter(Objects::nonNull)
                .flatMap(visitType -> visitType.generateOccurrences(targetMonth).stream())
                .filter(visit -> !excluded.contains(visit.getDate()))
                .sorted(Comparator.comparing(PlannedVisit::getDate)
                        .thenComparing(visit -> visit.getTimeSlot().getStartTime()))
                .collect(Collectors.toList());
    }

    private void detachVisitFromPlace(VisitType visitType) {
        Place place = visitType.getPlace();
        if (place == null || place.getPlaceTitle() == null) {
            return;
        }
        placeRepository.findById(place.getPlaceTitle()).ifPresent(loaded -> {
            loaded.removeVisit(visitType);
            placeRepository.save(loaded);
        });
    }

    private VisitType resolveVisitTypeByIdentifier(String identifier) {
        List<VisitType> allVisits = Optional.ofNullable(visitTypeRepository.findAll()).orElse(List.of());
        return visitTypeRepository.findById(identifier)
                .orElseGet(() -> allVisits.stream()
                        .filter(Objects::nonNull)
                        .filter(visit -> {
                            String visitId = visit.getId();
                            String visitTitle = visit.getVisitTitle();
                            return (visitId != null && identifier.equalsIgnoreCase(visitId))
                                    || (visitTitle != null && identifier.equalsIgnoreCase(visitTitle));
                        })
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Visita non trovata: " + identifier)));
    }

    private void purgeVisitFromVolunteers(String visitTypeId) {
        List<Volunteer> volunteers = new ArrayList<>(volunteerRepository.findAll());
        Set<String> removedVolunteers = new HashSet<>();
        for (Volunteer volunteer : volunteers) {
            if (volunteer == null) {
                continue;
            }
            boolean removedAttendance = volunteer.getVisitsAttending().removeIf(visit -> visit != null && visitTypeId.equals(visit.getId()));
            boolean removedShifts = purgeShiftsForVisit(volunteer, visitTypeId);
            if (!removedAttendance && !removedShifts) {
                continue;
            }
            if (volunteer.getVisitsAttending().isEmpty()) {
                removedVolunteers.add(volunteer.getNickname());
                continue;
            }
            volunteerRepository.save(volunteer);
        }
        for (String nickname : removedVolunteers) {
            volunteerService.removeVolunteerAccount(nickname);
        }
    }

    private boolean purgeShiftsForVisit(Volunteer volunteer, String visitTypeId) {
        boolean updated = false;
        for (YearMonth month : volunteer.getScheduledShifts().keySet()) {
            List<AssignedShift> shifts = new ArrayList<>(volunteer.getShiftsForMonth(month));
            if (shifts.removeIf(shift -> visitTypeId.equals(shift.getVisitTypeId()))) {
                updated = true;
                if (shifts.isEmpty()) {
                    volunteer.removeScheduledShifts(month);
                } else {
                    volunteer.assignShifts(month, shifts);
                }
            }
        }
        return updated;
    }

    private void promoteReviewPhaseIfNeeded() {
        settingsRepository.load().ifPresent(settings -> {
            if (settings.getPlanningPhase().ordinal() >= PlanningPhase.ASSIGNMENT.ordinal()
                    && settings.getPlanningPhase() != PlanningPhase.REVIEW) {
                settings.setPlanningPhase(PlanningPhase.REVIEW);
                settingsRepository.save(settings);
            }
        });
    }

    private void ensureCatalogAdditionCompatible(LocalDate validFrom) {
        settingsRepository.load().ifPresent(settings -> {
            PlanningPhase phase = settings.getPlanningPhase();
            YearMonth activeMonth = settings.getActivePlanningMonth();
            if (phase == null || activeMonth == null) {
                return;
            }
            if (phase.ordinal() < PlanningPhase.AVAILABILITY_COLLECTION_CLOSED.ordinal()) {
                return;
            }
            if (validFrom == null) {
                throw new IllegalArgumentException("Le nuove visite devono indicare una data di inizio validità");
            }
            YearMonth effectiveStart = YearMonth.from(validFrom);
            if (!effectiveStart.isAfter(activeMonth)) {
                throw new IllegalArgumentException(
                        "Le nuove visite introdotte dopo la chiusura devono essere valide dal mese successivo a " + activeMonth);
            }
        });
    }



}
