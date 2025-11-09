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

    private final PlaceRepository placeRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final VolunteerRepository volunteerRepository;
    private final SettingsRepository settingsRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final ConfiguratorRepository configuratorRepository;
    private final ProvisionedCredentialsRepository provisionedCredentialsRepository;
    private final VolunteerService volunteerService;
    private final Set<String> defaultCredentialsValidated = new HashSet<>();

    public ConfiguratorServiceImp(PlaceRepository placeRepository,
                                  VisitTypeRepository visitTypeRepository,
                                  VolunteerRepository volunteerRepository, SettingsRepository settingsRepository,
                                  MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                  ConfiguratorRepository configuratorRepository,
                                  ProvisionedCredentialsRepository provisionedCredentialsRepository,
                                  VolunteerService volunteerService) {
        this.placeRepository = Objects.requireNonNull(placeRepository, "Il repository dei luoghi non può essere nullo");
        this.visitTypeRepository = Objects.requireNonNull(visitTypeRepository, "Il repository dei tipi visita non può essere nullo");
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository, "Il repository dei volontari non può essere nullo");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "Il repository delle impostazioni non può essere nullo");
        this.monthlyVisitPlanRepository = Objects.requireNonNull(monthlyVisitPlanRepository, "Il repository dei piani mensili non può essere nullo");
        this.configuratorRepository = Objects.requireNonNull(configuratorRepository, "Il repository dei configuratori non può essere nullo");
        this.provisionedCredentialsRepository = Objects.requireNonNull(provisionedCredentialsRepository, "Il repository delle credenziali provisionate non può essere nullo");
        this.volunteerService = Objects.requireNonNull(volunteerService, "Il servizio dei volontari non può essere nullo");
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        String sanitized = sanitizeNickname(nickname);
        if (sanitized == null) {
            return false;
        }
        return provisionedCredentialsRepository.hasConfiguratorCredential(sanitized);
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        String sanitizedNickname = sanitizeNickname(nickname);
        if (sanitizedNickname == null) {
            throw new IllegalArgumentException("Il nickname di default non può essere vuoto");
        }
        String effectiveNickname = resolvePendingConfiguratorNickname(sanitizedNickname)
                .orElseThrow(() -> new IllegalStateException ("Le credenziali personali sono già state impostate"));

        String sanitizedPassword = requireNonBlank(password, "La password di default non può essere vuota");
        String expectedPassword = provisionedCredentialsRepository.findConfiguratorPassword(effectiveNickname)
                .orElseThrow(() -> new IllegalStateException("Credenziali di primo accesso non registrate"));

        if (!expectedPassword.equals(sanitizedPassword)) {
            throw new IllegalArgumentException("Credenziali di primo accesso non valide");
        }

        defaultCredentialsValidated.add(effectiveNickname);
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        String sanitizedDefault = sanitizeNickname(currentNickname);
        if (sanitizedDefault == null) {
            throw new IllegalArgumentException("Il nickname di default non può essere vuoto");
        }
        if (!isFirstAccessPending(sanitizedDefault)) {
            throw new IllegalStateException("Le credenziali sono già state configurate");
        }
        if (!defaultCredentialsValidated.contains(sanitizedDefault)) {
            throw new IllegalStateException("Credenziali di default non ancora verificate");
        }

        String sanitizedNickname = requireNonBlank(newNickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");

        if (isConfiguratorNicknameTaken(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        boolean matchesDefaultNickname = sanitizedNickname.equalsIgnoreCase(sanitizedDefault);
        if (!matchesDefaultNickname && provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }


        configuratorRepository.save(new Configurator(sanitizedNickname, sanitizedPassword));
        provisionedCredentialsRepository.consumeConfiguratorCredential(sanitizedDefault);
        defaultCredentialsValidated.remove(sanitizedDefault);
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

    private Optional<String> resolvePendingConfiguratorNickname(String sanitizedNickname) {
        if (sanitizedNickname == null) {
            return Optional.empty();
        }

        if (provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            return Optional.of(sanitizedNickname);
        }

        if (sanitizedNickname.length() <= 1) {
            return Optional.empty();
        }

        for (int index = 0; index < sanitizedNickname.length(); index++) {
            String candidate = sanitizedNickname.substring(0, index) + sanitizedNickname.substring(index + 1);
            if (candidate.isBlank()) {
                continue;
            }
            if (provisionedCredentialsRepository.hasConfiguratorCredential(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }


    private String sanitizeNickname(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isConfiguratorNicknameTaken(String nickname) {
        return configuratorRepository.load()
                .map(map -> map.values().stream()
                        .filter(Objects::nonNull)
                        .map(Configurator::getNickname)
                        .filter(Objects::nonNull)
                        .anyMatch(existing -> existing.equalsIgnoreCase(nickname)))
                .orElse(false);
    }

    @Override
    public void setTerritorialScope(String scope) {
        String s = Objects.requireNonNull(scope, "scope nullo");
        String trimmedScope = s.trim();
        if (trimmedScope.isEmpty()) {
            throw new IllegalArgumentException("scope vuoto");
        }

        var current = settingsRepository.load();
        if (current.isPresent()) {
            SystemSettings settings = current.get();
            String currentScope = settings.getTerritorialScope();
            if (currentScope == null) {
                settings.setTerritorialScope(trimmedScope);
                if (settings.getMaxPeoplePerSubscription() <= 0) {
                    settings.setMaxPeoplePerSubscription(15);
                }
                settingsRepository.save(settings);
                return;
            }

            if (!currentScope.equals(trimmedScope)) {
                throw new IllegalStateException(
                        "Territorial scope già definito come " + currentScope + " e non è modificabile");
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

        Optional<Place> existingPlace = placeRepository.findById(name);

        ensureCatalogChangeWindowAvailable();

        if (existingPlace.isPresent())
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

        Optional<Place> maybePlace = placeRepository.findById(placeID);
        Optional<VisitType> duplicateById = visitTypeRepository.findById(title);
        List<VisitType> existingVisits = Optional.ofNullable(visitTypeRepository.findAll()).orElse(List.of());

        ensureCatalogChangeWindowAvailable();
        ensureCatalogAdditionCompatible(validFrom);

        Place place = maybePlace.orElseThrow(() -> new IllegalArgumentException("Luogo non trovato"));
        if (duplicateById.isPresent()) {
            throw new IllegalArgumentException("Esiste già una visita con questo identificativo");
        }
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
        return "Visita inserita: %s (ID: %s)".formatted (title, visitType.getId ()) ;
    }

    @Override
    public void addVolunteer(String nickname, String defaultPassword) {
        ensureCatalogChangeWindowAvailable();
        String sanitizedNick = requireNonBlank (nickname, "nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank (defaultPassword, "password non può essere vuoto");
        if(volunteerRepository.findByNickname (sanitizedNick).isPresent()) throw new IllegalArgumentException ("Nickname già presente");
        Volunteer volunteer = new  Volunteer (sanitizedNick, sanitizedPassword);
        volunteer.setVisitsAttending (new ArrayList<> ());
        volunteerRepository.save(volunteer);
        provisionedCredentialsRepository.registerVolunteerCredential(sanitizedNick, sanitizedPassword);
    }

    @Override
    public void linkVOlunteerToVisit(String nickname, String visitTypeId) {
        ensureCatalogChangeWindowAvailable();
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
        ensureCatalogChangeWindowAvailable();
        change.run ();
    }

    @Override
    public MonthlyPlanDTO generateMonthlyPlan(YearMonth targetMonth) {
        Objects.requireNonNull (targetMonth, "Il mese di pianificazione non può essere nullo");
        SystemSettings settings = requireInitializedSettings();
        PlanningPhase phase = settings.getPlanningPhase();
        if (phase != PlanningPhase.AVAILABILITY_COLLECTION_CLOSED && phase != PlanningPhase.CATALOG_MANAGEMENT) {
            throw new IllegalStateException ("Il piano può essere generato solo dopo la chiusura della raccolta disponibilità");
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
        ensureCatalogChangeWindowAvailable();
        String sanitizedId = requireNonBlank(placeId, "L'identificativo del luogo non può essere vuoto");
        Place place = placeRepository.findById(sanitizedId)
                .orElseThrow(() -> new IllegalArgumentException("Luogo non trovato: " + sanitizedId));

        List<VisitType> visits = Optional.ofNullable(visitTypeRepository.findByPlace (sanitizedId)).orElse (List.of());
        List<String> visitIds = visits.stream()
                .filter(Objects::nonNull)
                .map(VisitType::getId)
                .filter(Objects::nonNull)
                .toList();

        for (VisitType visit : visits) {
            removeVisitTypeCascade(visit, false, false);
        }

        monthlyVisitPlanRepository.removePlannedVisitsByVisitTypes (visitIds);
        placeRepository.deleteById(sanitizedId);
        promoteReviewPhaseIfNeeded ();
    }

    @Override
    public void removeVisitType(String visitTypeId) {
        ensureCatalogChangeWindowAvailable();
        String sanitizedId = requireNonBlank(visitTypeId, "L'identificativo del tipo visita non può essere vuoto");
        VisitType visitType = resolveVisitTypeByIdentifier(sanitizedId);

        removeVisitTypeCascade(visitType, true, true);
    }

    @Override
    public void removeVolunteer(String nickname) {
        ensureCatalogChangeWindowAvailable();
        String sanitizedNick = requireNonBlank(nickname, "Il nickname non può essere vuoto");
        Volunteer volunteer = volunteerRepository.findByNickname(sanitizedNick)
                .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + sanitizedNick));

        Map<String, VisitType> visits = volunteer.getVisitsAttending().stream()
                .filter(Objects::nonNull)
                .map(visit -> visitTypeRepository.findById(visit.getId ()).orElse(visit))
                .filter(Objects::nonNull)
                .collect (Collectors.toMap (VisitType::getId, visit -> visit, (left,right) -> left, LinkedHashMap::new ));
        List<VisitType> orphanedVisits = new ArrayList<>();
        for (VisitType visit : visits.values ()) {
            if (visit.getGuides () != null) visit.getGuides().removeIf (guide -> sanitizedNick.equals(guide.getNickname ()));
            if (visit.getGuides () == null || visit.getGuides ().isEmpty ()){
                orphanedVisits.add(visit);
            }else visitTypeRepository.save(visit);
        }

        monthlyVisitPlanRepository.removeVolunteerAssignments (sanitizedNick);
        volunteerService.removeVolunteerAccount(sanitizedNick);
        for (VisitType visit : orphanedVisits.stream().collect(Collectors.toMap(VisitType::getId, v -> v, (left, right) -> left, LinkedHashMap::new)).values()) {
            removeVisitTypeCascade(visit, true, true);
        }

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
                plan.setAvailabilitySnapshots(Map.of());
                monthlyVisitPlanRepository.save(plan);
            });
            for (Volunteer volunteer : volunteerRepository.findAll()) {
                if (volunteer == null) {
                    continue;
                }
                if (volunteer.findAvailability(completedMonth).isPresent()) {
                    volunteer.removeAvailability(completedMonth);
                    volunteerRepository.save(volunteer);
                }
            }
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

    @Override
    public void registerConfigurator(String nickname, String password) {
        String sanitizedNickname = requireNonBlank(nickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");

        Map<String, Configurator> existing = configuratorRepository.load()
                .map(HashMap::new)
                .orElseGet(HashMap::new);

        boolean duplicate = existing.keySet().stream()
                .filter(Objects::nonNull)
                .anyMatch(registered -> registered.equalsIgnoreCase(sanitizedNickname));

        if (duplicate) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        if (provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        configuratorRepository.save(new Configurator(sanitizedNickname, sanitizedPassword));
    }

    @Override
    public List<String> listConfigurators() {
        return configuratorRepository.load()
                .map(map -> map.values().stream()
                        .filter(Objects::nonNull)
                        .map(Configurator::getNickname)
                        .filter(Objects::nonNull)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public boolean hasPendingConfiguratorSeeds() {
        return provisionedCredentialsRepository.hasAnyConfiguratorCredential();
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
        String visitId = visitType.getId();
        placeRepository.findById(place.getPlaceTitle()).ifPresent(loaded -> {
            List<VisitType> remaining = loaded.getVisits().stream()
                    .filter(Objects::nonNull)
                    .filter(candidate -> visitId == null || !visitId.equals(candidate.getId()))
                    .toList();
            loaded.setVisits(remaining);
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
        if (visitTypeId == null) {
            return;
        }
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
            monthlyVisitPlanRepository.removeVolunteerAssignments(nickname);
        }
    }

    private void removeVisitTypeCascade(VisitType visitType, boolean allowPlaceCascade, boolean removeFromPlans) {
        if (visitType == null) {
            return;
        }
        String visitId = visitType.getId();
        String placeId = Optional.ofNullable(visitType.getPlace())
                .map(Place::getPlaceTitle)
                .orElse(null);

        detachVisitFromPlace(visitType);
        purgeVisitFromVolunteers(visitId);
        if (removeFromPlans) {
            monthlyVisitPlanRepository.removePlannedVisitsByVisitType(visitId);
        }
        if (visitId != null) {
            visitTypeRepository.deleteById(visitId);
        }
        if (allowPlaceCascade && placeId != null) {
            placeRepository.findById(placeId).ifPresent(loaded -> {
                boolean hasVisits = loaded.getVisits().stream()
                        .anyMatch (Objects::nonNull);
                if (!hasVisits) {
                    placeRepository.deleteById(placeId);
                }
            });
        }
        promoteReviewPhaseIfNeeded();
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

    private void ensureCatalogChangeWindowAvailable(){
        settingsRepository.load().ifPresent(settings -> {
            YearMonth activeMonth = settings.getActivePlanningMonth ();
            if (activeMonth == null) {
                return;
            }
            PlanningPhase phase = settings.getPlanningPhase();
            if (phase.ordinal () < PlanningPhase.PLAN_GENERATED.ordinal ())
                throw new IllegalStateException ("Le modifiche al catalogo sono consentite solo dopo la generazione del piano mensile");
            if (phase == PlanningPhase.READY_FOR_NEXT_CYCLE || phase == PlanningPhase.AVAILABILITY_COLLECTION_OPEN)
                throw new IllegalStateException ("Le modifiche al catalogo non sono consentite dopo la riapertura della raccolta disponibilità");
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
