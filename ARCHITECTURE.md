# Architettura del Progetto — Tour Guide Planner

## Struttura a Strati

### Entity Layer (`model/`)
Modello dati gestito da JPA (Spring Data JPA / H2):
- `Place` — luogo fisico dove si svolgono le visite
- `VisitType` — tipo di visita ricorrente, con orari e capacità
- `PlannedVisit` — singola occorrenza pianificata di un VisitType
- `VisitBooking` — prenotazione di un beneficiario (@Embeddable, lista su PlannedVisit)
- `MonthlyVisitPlan` — piano mensile con le visite generate
- `Volunteer` — guida volontaria, con disponibilità mensili
- `MonthlyAvailability` — disponibilità di un volontario per un mese
- `Beneficiary` — utente finale che prenota le visite
- `SystemSettings` — impostazioni di sistema (ambito territoriale, max persone)
- `Configurator` — amministratore del sistema

**Locking ottimistico**: `SystemSettings` e `MonthlyVisitPlan` portano `@Version` per rilevare modifiche concorrenti.

**Cascade JPA**:
- `VisitType` → `PlannedVisit` (`CascadeType.REMOVE`, `orphanRemoval=true`)
- `PlannedVisit` → `VisitBooking` (`@ElementCollection`)

### Repository Layer (`repository/`)
Interfacce Spring Data JPA con query derivate e `@Query` custom:
- `PlannedVisitRepository`: query per data+status, per volontario, per place+data
- `VisitTypeRepository`, `PlaceRepository`, `VolunteerRepository`
- `MonthlyVisitPlanRepository`: `findByTargetMonth(YearMonth)`
- `SettingsRepository`, `ConfiguratorRepository`, `BeneficiaryRepository`

### Service Layer (`service/`)
Business logic con `@Transactional` su ogni operazione che modifica dati:
- `ConfiguratorServiceImp` — gestione territorio, luoghi, tipi visita, algoritmo di pianificazione
- `VolunteerServiceImp` — invio disponibilità con controllo finestra temporale
- `BeneficiaryBookingManager` — prenotazioni, cancellazioni, elenco visite disponibili
- `VisitStatusUpdateService` — job schedulato (`@Scheduled`) per transizioni PROPOSTA→CONFERMATA/CANCELLATA
- `ConfiguratorCredentialManager` — gestione credenziali configuratore

### Controller Layer (`controller/`)
REST API con Spring MVC:
- `ConfiguratorController` — `/api/configurator/*`, ruolo `CONFIGURATOR`
- `VolunteerController` — `/api/volunteers/*`, ruolo `VOLUNTEER`
- `BeneficiaryController` — `/api/beneficiaries/*`, ruolo `BENEFICIARY`

### Utility Layer (`util/`)
Classi statiche di policy (Strategy Pattern):
- `AvailabilitySubmissionPolicy` — controlla se la finestra di disponibilità è aperta
- `PlanningWindowPolicy` — calcola il prossimo mese pianificabile
- `DTOMapper` — conversione Entity ↔ DTO

### Security (`config/`)
- `SecurityConfig` — HTTP Basic Auth, `anyRequest().authenticated()`, `@EnableMethodSecurity`
- `CustomUserDetailsService` — ricerca utenti in Configurator/Volunteer/Beneficiary repository
- `DataSeeder` — dati demo al primo avvio (CommandLineRunner)

---

## Design Patterns

| Pattern | Dove |
|---|---|
| Strategy | `AvailabilitySubmissionPolicy` / `AvailabilitySubmissionStrategy` |
| DTO | Request/Response separati da Entity per tutti gli endpoint |
| Repository | Spring Data JPA con query custom |
| Optimistic Locking | `@Version` su `SystemSettings` e `MonthlyVisitPlan` |
| Global Exception Handler | `@RestControllerAdvice` `GlobalExceptionHandler` |
| Scheduled Task | `@Scheduled` cron su `VisitStatusUpdateService` |

---

## Sicurezza

- **Autenticazione**: Spring Security HTTP Basic
- **Autorizzazione**: `@PreAuthorize("hasRole('...')")` su ogni endpoint sensibile
- **Ruoli**: `ROLE_CONFIGURATOR`, `ROLE_VOLUNTEER`, `ROLE_BENEFICIARY`
- **Endpoint pubblici**: login, register, Swagger UI, H2 Console
- **Validazione input**: `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@AssertTrue` sui DTO
- **Nessun info leakage**: il `GlobalExceptionHandler` non espone `ex.getMessage()` per errori 500

---

## Flusso principale

```
Configuratore:
  POST /api/configurator/plans          → crea piano mensile
  POST /api/configurator/plans/{id}/generate  → genera visite (algoritmo anti-overlap)
  POST /api/configurator/plans/{id}/publish   → pubblica il piano

Volontario:
  POST /api/volunteers/{u}/availability → invia disponibilità (window check)

Beneficiario:
  GET  /api/beneficiaries/available-visits → vede visite PROPOSTA
  POST /api/beneficiaries/book-visit       → prenota (capacity check)
  DEL  /api/beneficiaries/bookings/{code}  → cancella prenotazione

Job schedulato (1:00 AM):
  VisitStatusUpdateService.updateVisitStatuses()
  → PROPOSTA → CONFERMATA se partecipanti >= minParticipants
  → PROPOSTA → CANCELLATA se partecipanti < minParticipants
```