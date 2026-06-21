# Testing Strategy — Tour Guide Planner

## Framework e Approccio

- **Unit tests (Service/Model)**: `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`
- **Controller tests**: `@WebMvcTest` + `@MockBean` + `@WithMockUser`
- **Utility tests**: test puri JUnit 5 su classi statiche
- **Dati di test**: oggetti in memoria (no DB), ID impostati con `ReflectionTestUtils.setField()`

---

## Test Esistenti

| Classe | Copertura | Note |
|---|---|---|
| `ConfiguratorServiceImpTest` | Algoritmo pianificazione | Verifica generazione visite per mese |
| `TerritoryImmutabilityTest` | `defineTerritorialScope()` | 2 scenari: set iniziale e tentativo modifica |
| `CascadingDeleteTest` | `removeVisitType()` | Pulizia associazioni volunteer + delete entity |
| `VisitStatusTransitionTest` | `updateVisitStatuses()` | CONFERMATA / CANCELLATA / nessuna prenotazione |
| `ConfiguratorAuthorizationTest` | `@PreAuthorize` | 403 per VOLUNTEER su endpoint CONFIGURATOR |
| `NonOverlapPlanningTest` | Algoritmo anti-overlap | Overlap → 1 visita; Non-overlap → 2 visite |
| `BeneficiaryServiceImpTest` | `BeneficiaryBookingManager` | Login, register, bookVisit, listVisits |
| `VolunteerServiceImpTest` | `VolunteerServiceImp.submitAvailability()` | Finestra aperta / giorni qualificati |
| `VolunteerControllerTest` | `VolunteerController` | Submit availability, assigned shifts |
| `AvailabilitySubmissionPolicyTest` | Policy isWindowOpen | Dentro/fuori finestra |
| `ExcludedDatesPolicyTest` | Date escluse | Politica blackout dates |
| `DTOMapperTest` | Mappatura DTO | Entity → DTO conversion |
| `PlaceTest`, `TimeSlotTest`, `VisitTypeTest`, `AssignedShiftTest` | Model | Costruttori e logica di dominio |

---

## Scenari Critici Testati

### 1. Immutabilità ambito territoriale
- ✅ Prima impostazione: `defineTerritorialScope("Milano")` → salvato
- ✅ Seconda impostazione: `defineTerritorialScope("Roma")` → `IllegalStateException`

### 2. Cascade delete (removeVisitType)
- ✅ Volunteer con 2 visite: rimuove solo la visita eliminata, salva il volontario
- ✅ Volunteer con 1 visita: rimuove la visita, elimina il volontario

### 3. Transizioni di stato visite
- ✅ 3 partecipanti, min 2 → CONFERMATA
- ✅ 2 partecipanti, min 5 → CANCELLATA
- ✅ 0 partecipanti, min 1 → CANCELLATA

### 4. Autorizzazione endpoint
- ✅ `VOLUNTEER` accede a `POST /api/configurator/plans` → 403 Forbidden
- ✅ `VOLUNTEER` accede a `POST /api/configurator/places` → 403 Forbidden
- ✅ `CONFIGURATOR` accede a `POST /api/configurator/plans` → non 403

### 5. Non-overlap nell'algoritmo di pianificazione
- ✅ VT1 (10:00-12:00) e VT2 (11:00-12:00) stesso luogo → solo VT1 pianificato
- ✅ VT1 (10:00-11:00) e VT2 (11:00-12:00) stesso luogo → entrambi pianificati

### 6. Finestra invio disponibilità
- ✅ Giorno 5 del mese precedente → finestra aperta, submission accettata
- ✅ Oggi (dopo giorno 15 del mese precedente) → `IllegalStateException`

---

## Comando per eseguire i test

```bash
mvn test
# Risultato atteso: BUILD SUCCESS, 0 failures
```

---

## Coverage Target

| Layer | Target |
|---|---|
| Service Layer | ≥ 70% |
| Controller Layer | ≥ 50% (metodi critici) |
| Utility/Policy | ≥ 90% |
| Entity/Model | ≥ 40% |

---

## Note per l'Esame Orale

- I test di integrazione non sono presenti (no `@SpringBootTest` con DB reale) — la verifica
  end-to-end si fa tramite Swagger UI o Postman con l'applicazione avviata.
- Il `DataSeeder` popola il DB ad ogni avvio con dati demo pronti per la dimostrazione.
- Credenziali demo: `admin/admin123` (configuratore), `mario.volontario/volontario123`,
  `anna.beneficiario/beneficiario123`.