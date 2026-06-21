package it.unibs.ingsw24_25.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.DTO.request.CreatePlaceRequest;
import it.unibs.ingsw24_25.DTO.request.CreateVisitTypeRequest;
import it.unibs.ingsw24_25.DTO.request.CreateVolunteerRequest;
import it.unibs.ingsw24_25.DTO.response.MonthlyPlanResponseDTO;
import it.unibs.ingsw24_25.DTO.response.PlaceResponse;
import it.unibs.ingsw24_25.DTO.response.VisitTypeResponse;
import it.unibs.ingsw24_25.DTO.response.VolunteerResponse;
import it.unibs.ingsw24_25.exception.InvalidCredentialsException;
import it.unibs.ingsw24_25.model.MonthlyVisitPlan;
import it.unibs.ingsw24_25.model.PlannedVisit;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.MonthlyVisitPlanRepository;
import it.unibs.ingsw24_25.repository.PlannedVisitRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.util.PlanningWindowPolicy;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configurator")
@Tag(name = "API Configuratore", description = "Endpoint per la gestione e configurazione del sistema da parte del configuratore.")
public class ConfiguratorController {
    private static final Logger log = LoggerFactory.getLogger(ConfiguratorController.class);

    private final ConfiguratorService configuratorService;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final PlannedVisitRepository plannedVisitRepository;
    private final VolunteerRepository volunteerRepository;

    public ConfiguratorController(ConfiguratorService configuratorService,
                                   MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                   PlannedVisitRepository plannedVisitRepository,
                                   VolunteerRepository volunteerRepository) {
        this.configuratorService = Objects.requireNonNull(configuratorService, "configuratorService non può essere nullo");
        this.monthlyVisitPlanRepository = Objects.requireNonNull(monthlyVisitPlanRepository, "monthlyVisitPlanRepository non può essere nullo");
        this.plannedVisitRepository = Objects.requireNonNull(plannedVisitRepository, "plannedVisitRepository non può essere nullo");
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository, "volunteerRepository non può essere nullo");
    }

    @GetMapping("/plans")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Elenca tutti i piani mensili", description = "Restituisce la lista di tutti i piani mensili.")
    public ResponseEntity<List<MonthlyPlanResponseDTO>> listPlans() {
        return ResponseEntity.ok(monthlyVisitPlanRepository.findAll().stream()
                .map(plan -> new MonthlyPlanResponseDTO(plan.getId(), plan.getTargetMonth(), plan.getPhase()))
                .collect(Collectors.toList()));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Inizializza un nuovo piano mensile", description = "Crea una nuova istanza di piano mensile per un dato mese.")
    public ResponseEntity<MonthlyPlanResponseDTO> initPlan(@RequestBody Map<String, YearMonth> request) {
        YearMonth targetMonth = request != null ? request.get("targetMonth") : null;
        if (targetMonth == null) {
            throw new IllegalArgumentException("targetMonth non può essere nullo");
        }
        if (targetMonth.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("Non è possibile pianificare un mese già passato");
        }
        // Validate targetMonth is within the acceptable planning horizon
        YearMonth recommended = PlanningWindowPolicy.resolveNextPlanningMonth(LocalDate.now());
        if (targetMonth.isBefore(recommended)) {
            throw new IllegalArgumentException("Il mese " + targetMonth + " è già trascorso o troppo vicino. " +
                    "Il prossimo mese pianificabile è: " + recommended);
        }
        MonthlyVisitPlan plan = new MonthlyVisitPlan(targetMonth);
        plan = monthlyVisitPlanRepository.save(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MonthlyPlanResponseDTO(plan.getId(), plan.getTargetMonth(), plan.getPhase()));
    }

    @PostMapping("/plans/{id}/preclusions")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Aggiunge date di preclusione a un piano", description = "Aggiunge un elenco di date in cui le visite non devono essere pianificate.")
    public ResponseEntity<Void> addPreclusions(@PathVariable Long id, @RequestBody List<LocalDate> excludedDates) {
        MonthlyVisitPlan plan = monthlyVisitPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Piano non trovato"));
        excludedDates.forEach(plan::addExcludedDate);
        monthlyVisitPlanRepository.save(plan);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/plans/{id}/generate")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Genera le visite per un piano mensile", description = "Attiva l'algoritmo di generazione delle visite per il piano specificato.")
    public ResponseEntity<Void> generateVisits(@PathVariable Long id) {
        configuratorService.generateMonthlyPlanVisits(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/plans/{id}/visits")
    @Operation(summary = "Elenca le visite generate per un piano", description = "Restituisce l'elenco delle visite pianificate per un dato piano mensile.")
    public ResponseEntity<List<VisitOccurrenceDTO>> listGeneratedVisits(@PathVariable Long id) {
        return ResponseEntity.ok(configuratorService.listPlannedVisitsWithStatus().stream()
                .filter(v -> v.getPlanId().equals(id))
                .collect(Collectors.toList()));
    }

    @PostMapping("/plans/{id}/publish")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Pubblica un piano mensile", description = "Rende un piano mensile visibile ai beneficiari per le prenotazioni.")
    public ResponseEntity<Void> publishMonthlyPlan(@PathVariable Long id) {
        configuratorService.publishMonthlyPlan(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending-seeds")
    @Operation(summary = "Controlla credenziali di default", description = "Verifica se esistono credenziali di primo accesso non ancora utilizzate per un configuratore.")
    public ResponseEntity<Boolean> hasPendingConfiguratorSeeds() {
        return ResponseEntity.ok(configuratorService.hasPendingConfiguratorSeeds());
    }

    @GetMapping
    @Operation(summary = "Elenca tutti i configuratori", description = "Restituisce la lista dei nickname di tutti i configuratori registrati.")
    public ResponseEntity<List<String>> listConfigurators() {
        return ResponseEntity.ok(configuratorService.listConfigurators());
    }

    @PostMapping("/login")
    @Operation(summary = "Login per configuratore", description = "Verifica le credenziali di accesso per un configuratore.")
    public ResponseEntity<Map<String, Object>> verifyLogin(@RequestBody Map<String, String> credentials) {
        try {
            String nickname = credentials.getOrDefault("nickname", credentials.get("username"));
            String password = credentials.get("password");
            if (!configuratorService.verifyLogin(nickname, password)) {
                throw new InvalidCredentialsException("Credenziali non valide");
            }
            log.info("Login riuscito per configuratore: {}", nickname);
            return ResponseEntity.ok(Map.of("success", true, "mustChangePassword", false));
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante il login del configuratore", e);
            throw new IllegalStateException("Errore interno durante il login.");
        }
    }

    @GetMapping("/settings")
    @Operation(summary = "Ottieni le impostazioni di sistema", description = "Restituisce l'oggetto SystemSettings corrente.")
    public ResponseEntity<SystemSettingsDTO> getSystemSettings() {
        SystemSettingsDTO settings = configuratorService.getSystemSettings();
        if (settings == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Definisci l'ambito territoriale", description = "Permette di definire l'ambito territoriale.")
    public ResponseEntity<Void> defineTerritorialScope(@RequestBody Map<String, String> request) {
        configuratorService.defineTerritorialScope(request.get("scope"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/places")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Aggiunge un nuovo luogo", description = "Crea un nuovo luogo (es. museo, parco) dove si possono svolgere le visite.")
    public ResponseEntity<Map<String, String>> addPlace(@Valid @RequestBody CreatePlaceRequest request) {
        String result = configuratorService.addPlace(request.title(), "", request.location());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result));
    }

    @PostMapping("/visits")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Aggiunge un nuovo tipo di visita", description = "Crea un nuovo tipo di visita associato a un luogo, definendone orari, capacità e periodo di validità.")
    public ResponseEntity<String> addVisitType(@Valid @RequestBody CreateVisitTypeRequest request) {
        List<TimeSlot> schedules = request.schedules().stream()
                .map(scheduleRequest -> new TimeSlot(
                        scheduleRequest.dayOfWeek(),
                        scheduleRequest.startTime(),
                        scheduleRequest.durationInMinutes()))
                .collect(Collectors.toList());

        String result = configuratorService.addVisitType(request.placeId(), request.title(), request.description(),
                request.meetingPoint(), schedules, request.ticketRequired(), request.minParticipants(),
                request.maxParticipants(), request.startDate(), request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/volunteers")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Registra un nuovo volontario", description = "Aggiunge un nuovo volontario al sistema e gli assegna una password di default per il primo accesso.")
    public ResponseEntity<Void> addVolunteer(@Valid @RequestBody CreateVolunteerRequest request) {
        configuratorService.addVolunteer(request.nickname(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/volunteers/{nickname}/visits/{visitId}")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Abilita un volontario a una visita", description = "Associa un volontario a un tipo di visita, abilitandolo a guidarla.")
    public ResponseEntity<Void> linkVolunteerToVisit(@PathVariable String nickname, @PathVariable String visitId) {
        configuratorService.linkVolunteerToVisit(nickname, visitId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/places")
    @Operation(summary = "Elenca tutti i luoghi", description = "Restituisce una lista di tutti i luoghi configurati nel sistema.")
    public ResponseEntity<List<PlaceResponse>> listPlace() {
        return ResponseEntity.ok(configuratorService.listPlace());
    }

    @GetMapping("/places/{placeId}/visits")
    @Operation(summary = "Elenca visite per luogo", description = "Restituisce tutti i tipi di visita associati a un luogo specifico.")
    public ResponseEntity<List<VisitTypeResponse>> listVisitTypeByPlace(@PathVariable String placeId) {
        return ResponseEntity.ok(configuratorService.listVisitTypeByPlace(placeId));
    }

    @GetMapping("/visits")
    @Operation(summary = "Elenca tutti i tipi di visita", description = "Restituisce una lista completa di tutti i tipi di visita configurati.")
    public ResponseEntity<List<VisitTypeResponse>> listVisitType() {
        return ResponseEntity.ok(configuratorService.listVisitType());
    }

    @GetMapping("/volunteers")
    @Operation(summary = "Elenca volontari e visite associate", description = "Restituisce una lista di tutti i volontari e i tipi di visita a cui sono abilitati.")
    public ResponseEntity<List<VolunteerResponse>> listVolunteerWithVisitType() {
        return ResponseEntity.ok(configuratorService.listVolunteerWVisitType());
    }

    @PostMapping("/settings/blackout-dates")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Imposta date di chiusura", description = "Definisce le date in cui il servizio non è attivo e non possono essere pianificate visite.")
    public ResponseEntity<Void> setBlackoutDates(@RequestBody List<LocalDate> dates) {
        configuratorService.setBlackoutDates(dates);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/settings/max-people")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Imposta capienza massima per prenotazione", description = "Definisce il numero massimo di persone che un singolo beneficiario può includere in una prenotazione.")
    public ResponseEntity<Void> setMaxPeoplePerSubscription(@RequestBody Map<String, Integer> request) {
        configuratorService.setMaxPeoplePerSubscription(request.get("max"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/places/{placeId}")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Rimuove un luogo", description = "Elimina un luogo e tutte le visite associate. L'operazione è consentita solo in determinati stati del sistema.")
    public ResponseEntity<Void> removePlace(@PathVariable String placeId) {
        configuratorService.removePlace(placeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/visits/{visitTypeId}")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Rimuove un tipo di visita", description = "Elimina un tipo di visita. L'operazione è consentita solo in determinati stati del sistema.")
    public ResponseEntity<Void> removeVisitType(@PathVariable String visitTypeId) {
        configuratorService.removeVisitType(visitTypeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/volunteers/{volunteer}")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Rimuove un volontario", description = "Elimina un volontario dal sistema. L'operazione è consentita solo in determinati stati del sistema.")
    public ResponseEntity<Void> removeVolunteer(@PathVariable String volunteer) {
        configuratorService.removeVolunteer(volunteer);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-default-credentials")
    @Operation(summary = "Verifica credenziali di default", description = "Controlla che le credenziali di primo accesso fornite a un configuratore siano valide.")
    public ResponseEntity<Void> verifyDefaultCredentials(@RequestBody Map<String, String> credentials) {
        String nickname = credentials.get("nickname");
        String password = credentials.get("password");
        configuratorService.verifyDefaultCredentials(nickname, password);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/credentials")
    @Operation(summary = "Imposta credenziali personali", description = "Consente a un configuratore di sostituire le credenziali di default con una password personale dopo averle verificate.")
    public ResponseEntity<Void> setPersonalCredentials(@RequestBody Map<String, String> credentials) {
        String defaultNickname = credentials.get("defaultNickname");
        String nickname = credentials.get("nickname");
        String password = credentials.get("password");
        configuratorService.setPersonalCredentials(defaultNickname, nickname, password);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/settings/territorial-scope")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Imposta l'ambito territoriale", description = "Definisce l'ambito territoriale di operatività del sistema (es. una città o provincia). Può essere impostato solo una volta.")
    public ResponseEntity<Void> setTerritorialScope(@RequestBody Map<String, String> request) {
        configuratorService.setTerritorialScope(request.get("scope"));
        return ResponseEntity.ok().build();
    }



    @PutMapping("/change-password")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Operation(summary = "Cambia la password del configuratore", description = "Consente al configuratore di cambiare la propria password.")
    public ResponseEntity<Void> changePassword(@RequestBody Map<String, String> request) {
        configuratorService.changePassword(request.get("username"), request.get("oldPassword"), request.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/planned-visits/{visitId}/available-volunteers")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Transactional(readOnly = true)
    @Operation(summary = "Volontari disponibili per una visita", description = "Restituisce i volontari che hanno disponibilità nel giorno della visita pianificata.")
    public ResponseEntity<List<VolunteerResponse>> getAvailableVolunteers(@PathVariable Long visitId) {
        PlannedVisit visit = plannedVisitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visita pianificata non trovata: " + visitId));
        LocalDate date = visit.getVisitDate();
        YearMonth month = YearMonth.from(date);
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<VolunteerResponse> available = volunteerRepository.findAll().stream()
                .filter(v -> v.getAvailabilities().stream()
                        .anyMatch(a -> a.getReferenceMonth().equals(month)
                                && a.getPreferredDays().contains(dayOfWeek)))
                .map(v -> new VolunteerResponse(v.getNickname(), v.isActive(), v.isFirstAccessPending()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(available);
    }

    @PutMapping("/planned-visits/{visitId}/volunteer")
    @PreAuthorize("hasRole('CONFIGURATOR')")
    @Transactional
    @Operation(summary = "Assegna un volontario a una visita pianificata", description = "Associa un volontario a una specifica occorrenza di visita pianificata.")
    public ResponseEntity<Void> assignVolunteerToPlannedVisit(@PathVariable Long visitId,
                                                               @RequestBody Map<String, String> request) {
        PlannedVisit visit = plannedVisitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visita pianificata non trovata: " + visitId));
        String nickname = request.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            visit.setVolunteer(null);
        } else {
            Volunteer volunteer = volunteerRepository.findByNickname(nickname)
                    .orElseThrow(() -> new EntityNotFoundException("Volontario non trovato: " + nickname));
            visit.setVolunteer(volunteer);
        }
        plannedVisitRepository.save(visit);
        return ResponseEntity.ok().build();
    }
}
