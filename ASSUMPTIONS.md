# Assunzioni Architetturali — Tour Guide Planner

## 1. Finestra di Invio Disponibilità

**Assunzione**: La finestra di raccolta disponibilità per il mese M è aperta dal
giorno 1 al giorno 15 del mese M-1.

**Esempio**: per pianificare agosto 2026, i volontari devono inviare la disponibilità
tra il 1° e il 15 luglio 2026.

**Implementazione**: `AvailabilitySubmissionPolicy.isWindowOpen(YearMonth, LocalDate)`
che delega a `StandardAvailabilitySubmissionStrategy`.

**Impatto**: `VolunteerServiceImp.submitAvailability()` lancia `IllegalStateException`
se chiamato fuori finestra.

---

## 2. Calcolo Prossimo Mese Pianificabile

**Assunzione**: Il prossimo mese pianificabile dipende dal giorno corrente:
- Giorno ≤ 15: si può pianificare il mese corrente+2
- Giorno > 15: si può pianificare il mese corrente+3

**Esempio**: il 10 giugno → si pianifica agosto; il 20 giugno → si pianifica settembre.

**Implementazione**: `PlanningWindowPolicy.resolveNextPlanningMonth(LocalDate)`.

**Impatto**: `ConfiguratorController.initPlan()` rifiuta mesi prima di questa soglia.

---

## 3. Non-Overlap Visite per Luogo

**Assunzione**: Due tipi di visita nello stesso luogo fisico non possono essere
pianificati nello stesso giorno se i loro intervalli orari si sovrappongono.

**Regola di overlap**: due intervalli `[A_start, A_end)` e `[B_start, B_end)` si
sovrappongono se e solo se `NOT (A_end ≤ B_start OR B_end ≤ A_start)`.

**Implementazione**: check in `ConfiguratorServiceImp.generateMonthlyPlanVisits()`,
loop su `generatedVisits` per lo stesso luogo e giorno.

**Impatto**: in caso di conflitto, il secondo tipo di visita viene saltato per quel giorno.
Il primo tipo (nell'ordine di `visitTypeRepository.findAll()`) ha la priorità.

---

## 4. Frequenza Settimanale Volontari

**Assunzione**: Il campo `weeklyFrequency` di `MonthlyAvailability` esprime il numero
massimo di turni che il volontario è disponibile a svolgere in una settimana ISO.
Se `weeklyFrequency ≤ 0`, non c'è limite.

**Implementazione**: mappa `volunteerShiftsByWeek` (volunteerId → weekISO → count) in
`generateMonthlyPlanVisits()`, aggiornata dopo ogni assegnazione.

---

## 5. Transizioni di Stato delle Visite

**Ciclo di vita di `PlannedVisit.status`**:

```
PROPOSTA → COMPLETA    (capacità massima raggiunta dopo prenotazione)
COMPLETA → PROPOSTA    (prenotazione cancellata, posti liberi di nuovo)
PROPOSTA → CONFERMATA  (job schedulato 3gg prima: partecipanti ≥ minParticipants)
PROPOSTA → CANCELLATA  (job schedulato 3gg prima: partecipanti < minParticipants)
```

Le transizioni PROPOSTA→CONFERMATA e PROPOSTA→CANCELLATA sono irreversibili
una volta eseguite dal job (`VisitStatusUpdateService`, cron 1:00 AM giornaliero).

---

## 6. Cascade Delete

**Assunzione**: L'eliminazione di entità parent propaga agli entity figlio:

| Parent eliminato | Effetto |
|---|---|
| `Place` | Pulisce `volunteer.visitsAttending`, poi elimina `VisitType` (cascade JPA) |
| `VisitType` | Rimuove da `volunteer.visitsAttending`, elimina voluntario se lista vuota; rimuove da `place.visits`, elimina place se lista vuota |
| `Volunteer` | Null-ifica `plannedVisit.volunteer` (invece di cascade delete) |
| `PlannedVisit` | Elimina i `VisitBooking` associati (`@ElementCollection`) |

**Nota**: l'eliminazione del volontario NON elimina le `PlannedVisit` — viene
solo rimosso il riferimento al volontario per mantenere la storia delle visite.

---

## 7. Autenticazione e Ruoli

**Assunzione**: Ogni utente appartiene a una sola categoria (Configurator / Volunteer /
Beneficiary). Il `CustomUserDetailsService` cerca nell'ordine:
1. `ConfiguratorRepository.findByNickname()` → `ROLE_CONFIGURATOR`
2. `VolunteerRepository.findByNickname()` → `ROLE_VOLUNTEER`
3. `BeneficiaryRepository.findByUsername()` → `ROLE_BENEFICIARY`

**Implicazione**: i nickname devono essere unici tra le tre categorie per evitare
ambiguità di autenticazione.

---

## 8. Immutabilità dell'Ambito Territoriale

**Assunzione**: L'ambito territoriale (`SystemSettings.territorialScope`) può essere
impostato una sola volta (`defineTerritorialScope()`). Qualsiasi tentativo successivo
di modificarlo lancia `IllegalStateException`.

Il metodo `setTerritorialScope()` (usato solo nell'init del sistema) permette
invece la riconfigurazione se il valore è invariato.