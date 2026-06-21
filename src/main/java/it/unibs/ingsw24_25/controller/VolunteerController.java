package it.unibs.ingsw24_25.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unibs.ingsw24_25.DTO.AssignedShiftDTO;
import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.DTO.request.SubmitAvailabilityRequest;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.MonthlyVisitPlan;
import it.unibs.ingsw24_25.model.PlannedVisit;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.exception.InvalidCredentialsException;
import it.unibs.ingsw24_25.repository.PlannedVisitRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import it.unibs.ingsw24_25.service.VolunteerService;
import it.unibs.ingsw24_25.util.DTOMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/volunteers")
@Tag(name = "API Volontario", description = "Endpoint per la gestione delle operazioni del volontario.")
public class VolunteerController {
    private static final Logger log = LoggerFactory.getLogger(VolunteerController.class);

    private final VolunteerService volunteerService;
    private final VolunteerRepository volunteerRepository;
    private final PlannedVisitRepository plannedVisitRepository;

    public VolunteerController(VolunteerService volunteerService,
                                VolunteerRepository volunteerRepository,
                                PlannedVisitRepository plannedVisitRepository) {
        this.volunteerService = volunteerService;
        this.volunteerRepository = volunteerRepository;
        this.plannedVisitRepository = plannedVisitRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Login per volontario", description = "Verifica le credenziali di accesso per un volontario.")
    public ResponseEntity<Map<String, Object>> verifyLogin(@RequestBody Map<String, String> credentials) {
        try {
            String nickname = credentials.get("nickname");
            String password = credentials.get("password");
            if (!volunteerService.verifyLogin(nickname, password)) {
                throw new InvalidCredentialsException("Credenziali non valide");
            }
            boolean mustChangePassword = volunteerService.isFirstAccessPending(nickname);
            return ResponseEntity.ok(Map.of("success", true, "mustChangePassword", mustChangePassword));
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante il login del volontario", e);
            throw new IllegalStateException("Errore interno durante il login.");
        }
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('VOLUNTEER')")
    @Operation(summary = "Cambia la password del volontario")
    public ResponseEntity<Void> changePassword(@RequestBody Map<String, String> request) {
        volunteerService.changePassword(request.get("username"), request.get("oldPassword"), request.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{nickname}/shifts")
    @PreAuthorize("hasRole('VOLUNTEER')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Operation(summary = "Turni assegnati al volontario per mese", description = "Restituisce le visite pianificate assegnate al volontario, opzionalmente filtrate per mese (YYYY-MM).")
    public ResponseEntity<List<VisitOccurrenceDTO>> getShiftsByMonth(@PathVariable String nickname,
                                                                      @RequestParam(required = false) String month) {
        Volunteer volunteer = volunteerRepository.findByNickname(nickname)
                .orElseThrow(() -> new EntityNotFoundException("Volontario non trovato: " + nickname));

        List<PlannedVisit> visits = plannedVisitRepository.findByVolunteer(volunteer);

        if (month != null && !month.isBlank()) {
            YearMonth ym = YearMonth.parse(month);
            visits = visits.stream()
                    .filter(v -> YearMonth.from(v.getVisitDate()).equals(ym))
                    .collect(Collectors.toList());
        }

        List<VisitOccurrenceDTO> result = visits.stream()
                .map(v -> DTOMapper.toVisitOccurrenceDTO(v.getMonthlyPlan(), v, v.getVisitType(), true))
                .sorted(java.util.Comparator.comparing(VisitOccurrenceDTO::getDate)
                        .thenComparing(dto -> dto.getStartTime() != null ? dto.getStartTime().toString() : ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{username}/availability")
    @PreAuthorize("hasRole('VOLUNTEER')")
    @Operation(summary = "Invia disponibilità mensile", description = "Permette a un volontario di inviare la propria disponibilità per un dato mese.")
    public ResponseEntity<Void> submitAvailability(@PathVariable String username, @Valid @RequestBody SubmitAvailabilityRequest request) {
        MonthlyAvailability availability = new MonthlyAvailability(
                request.month(),
                new java.util.HashSet<>(request.availableDays()),
                request.availableDays().size(),
                LocalDate.now(),
                false,
                null
        );
        volunteerService.submitAvailability(username, availability, LocalDate.now());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/assigned-visits")
    @PreAuthorize("hasRole('VOLUNTEER')")
    @Operation(summary = "Elenca le visite assegnate al volontario", description = "Restituisce una lista di tutte le visite a cui il volontario è stato assegnato.")
    public ResponseEntity<List<AssignedShiftDTO>> getAssignedVisits(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return ResponseEntity.ok(volunteerService.getAssignedShifts(username));
    }
}