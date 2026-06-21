package it.unibs.ingsw24_25.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.DTO.request.BookVisitRequest;
import it.unibs.ingsw24_25.DTO.request.CreateBeneficiaryRequest;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.service.BeneficiaryService;
import it.unibs.ingsw24_25.exception.InvalidCredentialsException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiaries")
@Tag(name = "API Beneficiario", description = "Endpoint per la gestione delle operazioni del beneficiario.")
public class BeneficiaryController {
    private static final Logger log = LoggerFactory.getLogger(BeneficiaryController.class);

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login per beneficiario", description = "Verifica le credenziali di accesso per un beneficiario.")
    public ResponseEntity<Map<String, Object>> verifyLogin(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            if (!beneficiaryService.verifyLogin(username, password)) {
                throw new InvalidCredentialsException("Credenziali non valide");
            }
            return ResponseEntity.ok(Map.of("success", true, "mustChangePassword", false));
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante il login del beneficiario", e);
            throw new IllegalStateException("Errore interno durante il login.");
        }
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('BENEFICIARY')")
    @Operation(summary = "Cambia la password del beneficiario")
    public ResponseEntity<Void> changePassword(@RequestBody Map<String, String> request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        beneficiaryService.changePassword(userDetails.getUsername(), request.get("oldPassword"), request.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    @Operation(summary = "Registra un nuovo beneficiario", description = "Crea un nuovo account per un beneficiario.")
    public ResponseEntity<Void> register(@Valid @RequestBody CreateBeneficiaryRequest request) {
        beneficiaryService.register(request.fullName(), request.username(), request.password());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/available-visits")
    @Operation(summary = "Elenca le visite disponibili", description = "Restituisce una lista di visite pianificate che sono disponibili per la prenotazione.")
    public ResponseEntity<List<VisitOccurrenceDTO>> listAvailableVisits() {
        return ResponseEntity.ok(beneficiaryService.listVisitsByStatus(VisitStatus.PROPOSTA));
    }

    @PostMapping("/book-visit")
    @PreAuthorize("hasRole('BENEFICIARY')")
    @Operation(summary = "Prenota una visita", description = "Crea una nuova prenotazione per un beneficiario per una data visita.")
    public ResponseEntity<String> bookVisit(@AuthenticationPrincipal UserDetails userDetails,
                                             @Valid @RequestBody BookVisitRequest request) {
        String username = userDetails.getUsername();
        String bookingCode = beneficiaryService.bookVisit(
                username, request.visitId(), request.participants(),
                request.notes() != null ? request.notes() : "", LocalDate.now());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(bookingCode);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('BENEFICIARY')")
    @Operation(summary = "Elenca le prenotazioni del beneficiario", description = "Restituisce una lista di tutte le prenotazioni effettuate dal beneficiario corrente.")
    public ResponseEntity<List<VisitBookingDTO>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        return ResponseEntity.ok(beneficiaryService.listBookings(username));
    }

    @DeleteMapping("/bookings/{bookingCode}")
    @PreAuthorize("hasRole('BENEFICIARY')")
    @Operation(summary = "Cancella una prenotazione", description = "Permette a un beneficiario di cancellare una delle sue prenotazioni.")
    public ResponseEntity<Void> cancelBooking(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String bookingCode) {
        String username = userDetails.getUsername();
        beneficiaryService.cancelBooking(username, bookingCode, LocalDate.now());
        return ResponseEntity.noContent().build();
    }
}