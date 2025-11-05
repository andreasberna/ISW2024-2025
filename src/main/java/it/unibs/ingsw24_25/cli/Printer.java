package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.model.PlanningPhase;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.VisitState;

import java.io.PrintStream;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;


public class Printer {
    public static final String PLACE_HEADER = "Luoghi disponibili:";
    public static final String PLACE_EMPTY_MESSAGE = "Nessun luogo registrato al momento";
    public static final String VISIT_TYPE_HEADER = "Visite disponibili:";
    public static final String VISIT_TYPE_EMPTY_MESSAGE = "Nessuna visita registrata al momento";
    public static final String VOLUNTEER_HEADER = "Volontari disponibili:";
    public static final String VOLUNTEER_EMPTY_MESSAGE = "Nessun volontario disponibile al momento";
    public static final String PLAN_HEADER = "Piano mensile:";
    public static final String PLAN_EMPTY_VISITS_MESSAGE = "Nessuna visita pianificata per il mese selezionato.";
    public static final String SNAPSHOT_HEADER = "Disponibilità bloccate:";
    public static final String SNAPSHOT_EMPTY_MESSAGE = "Nessuna disponibilità bloccata.";

    private final PrintStream out;

    public Printer(PrintStream out) {
        this.out = Objects.requireNonNull(out, "PrintStream non può essere nullo");
    }

    public void println(String text) {
        out.println(text);
    }

    public void print(String text) {
        out.print(text);
    }

    public void printPlaceList(List<PlaceDTO> places) {
        if (places == null || places.isEmpty()) {
            out.println(PLACE_EMPTY_MESSAGE);
            return;
        }

        places.stream()
                .map(this::format)
                .forEach(out::println);
    }

    public void printVisitTypeList(List<VisitTypeDTO> visitTypes) {
        if (visitTypes == null || visitTypes.isEmpty()) {
            out.println(VISIT_TYPE_EMPTY_MESSAGE);
            return;
        }

        visitTypes.stream()
                .map(this::format)
                .forEach(out::println);
    }

    public void printVolunteerList(List<VolunteerDTO> volunteers) {
        if (volunteers == null || volunteers.isEmpty()) {
            out.println(VOLUNTEER_EMPTY_MESSAGE);
            return;
        }

        volunteers.stream()
                .map(this::format)
                .forEach(out::println);
    }

    public void printAvailabilitySnapshots(List<PlanAvailabilityDTO> snapshots) {
        out.println(SNAPSHOT_HEADER);
        if (snapshots == null || snapshots.isEmpty()) {
            out.println(SNAPSHOT_EMPTY_MESSAGE);
            return;
        }

        snapshots.stream()
                .map(this::formatPlanAvailability)
                .forEach(out::println);
    }

    public void printMonthlyPlan(MonthlyPlanDTO plan) {
        out.println(PLAN_HEADER);
        if (plan == null) {
            out.println(PLAN_EMPTY_VISITS_MESSAGE);
            return;
        }

        out.println("Mese: " + plan.getTargetMonth());
        out.println("Fase: " + formatPlanningPhase(plan.getPhase()));
        if (plan.getAvailabilityWindowClosedOn() != null) {
            out.println("Finestra disponibilità chiusa il: " + plan.getAvailabilityWindowClosedOn());
        }

        List<PlannedVisitDTO> visits = plan.getPlannedVisits();
        if (visits == null || visits.isEmpty()) {
            out.println(PLAN_EMPTY_VISITS_MESSAGE);
        } else {
            visits.stream()
                    .map(this::formatPlannedVisit)
                    .forEach(out::println);
        }
    }

    private String format(PlaceDTO place) {
        if (place == null) {
            return "";
        }

        return ("Luogo: %s%nDescrizione: %s%nUbicazione: %s" + System.lineSeparator())
                .formatted(
                        safe(place.getTitle()),
                        safe(place.getDescription()),
                        safe(place.getLocation())
                );
    }

    private String format(VisitTypeDTO visitType) {
        if (visitType == null) {
            return "";
        }

        return ("Visita: %s%nID: %s%nGiorni: %s%nOrario: %s - %s%nDurata: %d minuti%nTicket richiesto: %s%nPartecipanti: %d-%d%nStato: %s" + System.lineSeparator())
                .formatted(
                        safe(visitType.getTitle()),
                        safe(visitType.getId()),
                        safe(visitType.getDaySummary()),
                        safe(visitType.getStartTime()),
                        safe(visitType.getendTime()),
                        visitType.getDurationMinutes(),
                        visitType.isTicketRequired() ? "Sì" : "No",
                        visitType.getMinParticipants(),
                        visitType.getMaxParticipants(),
                        formatState(visitType.getState ())
                );
    }

    private String format(VolunteerDTO volunteer) {
        if (volunteer == null) {
            return "";
        }

        String visits = volunteer.getVisitTypeTitles() == null || volunteer.getVisitTypeTitles().isEmpty()
                ? "Nessuna visita assegnata"
                : volunteer.getVisitTypeTitles().stream()
                .map(this::safe)
                .collect(Collectors.joining(", "));

        String credentialStatus = volunteer.isFirstAccessPending()
                ? "Credenziali personali da definire"
                : "Credenziali personali definite";

        String availabilitySection = formatAvailabilities(volunteer.getAvailabilities());
        String shiftSection = formatShifts(volunteer.getScheduledShifts());

        return ("Volontario: %s%nStato credenziali: %s%nVisite: %s%nDisponibilità:%n%s%nTurni assegnati:%n%s" + System.lineSeparator())
                .formatted(
                        safe(volunteer.getNickname()),
                        credentialStatus,
                        visits,
                        availabilitySection,
                        shiftSection
                );
    }

    private String formatPlanAvailability(PlanAvailabilityDTO snapshot) {
        if (snapshot == null) {
            return "";
        }
        String days = snapshot.getPreferredDays() == null || snapshot.getPreferredDays().isEmpty()
                ? "-"
                : snapshot.getPreferredDays().stream()
                .sorted()
                .map(day -> capitalize(day.getDisplayName(TextStyle.FULL, Locale.ITALIAN)))
                .collect(Collectors.joining(", "));

        String captured = snapshot.getSnapshotCapturedOn() == null
                ? "-"
                : snapshot.getSnapshotCapturedOn().toString();

        return "Volontario: %s | Mese: %s | Giorni: %s | Frequenza: %d | Inviata il: %s | Snapshot il: %s"
                .formatted(
                        safe(snapshot.getVolunteerNickname()),
                        snapshot.getReferenceMonth(),
                        days,
                        snapshot.getWeeklyFrequency(),
                        snapshot.getSubmittedOn(),
                        captured
                );
    }

    private String formatPlannedVisit(PlannedVisitDTO visit) {
        if (visit == null) {
            return "";
        }
        String dayLabel = visit.getDayOfWeek() == null
                ? "-"
                : capitalize(visit.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ITALIAN));
        String startLabel = visit.getStartTime() == null ? "-" : visit.getStartTime().toString();
        String volunteers = visit.getAssignedVolunteers() == null || visit.getAssignedVolunteers().isEmpty()
                ? "-"
                : visit.getAssignedVolunteers().stream()
                .map(this::safe)
                .collect(Collectors.joining(", "));
        String title = visit.getVisitTitle() == null ? visit.getVisitTypeId() : visit.getVisitTitle();

        return "- %s (%s) | Tipo: %s [%s] | Inizio: %s | Durata: %d minuti | Proponibile: %s | Volontari: %s"
                .formatted(
                        visit.getDate(),
                        dayLabel,
                        title,
                        safe(visit.getVisitTypeId()),
                        startLabel,
                        visit.getDurationMinutes(),
                        visit.isProposable() ? "Sì" : "No",
                        volunteers
                );
    }

    private String formatPlanningPhase(PlanningPhase phase) {
        if (phase == null) {
            return "-";
        }
        String label = phase.name().toLowerCase(Locale.ITALIAN).replace('_', ' ');
        return capitalize(label);
    }

    private String formatAvailabilities(List<VolunteerAvailabilityDTO> availabilities){
        if (availabilities == null || availabilities.isEmpty()) {
            return "  Nessuna disponibilità registrata";
        }

        return availabilities.stream()
                .sorted((left, right) -> left.getReferenceMonth().compareTo(right.getReferenceMonth()))
                .map(this::formatAvailability)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatAvailability(VolunteerAvailabilityDTO availability){
        if (availability == null) {
            return "";
        }
        String days = availability.getPreferredDays() == null || availability.getPreferredDays().isEmpty()
                ? "-"
                : availability.getPreferredDays().stream()
                .sorted()
                .map(day -> capitalize(day.getDisplayName(TextStyle.FULL, Locale.ITALIAN)))
                .collect(Collectors.joining(", "));

        return "  - %s | Giorni: %s | Frequenza settimanale: %d | Inviata il: %s"
                .formatted(
                        availability.getReferenceMonth(),
                        days,
                        availability.getWeeklyFrequency(),
                        availability.getSubmittedOn()
                );
    }

    private String formatShifts(List<AssignedShiftDTO> shifts){
        if (shifts == null || shifts.isEmpty()) {
            return "  Nessun turno assegnato";
        }

        return shifts.stream()
                .map(this::formatShift)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatShift(AssignedShiftDTO shift){
        if (shift == null) {
            return "";
        }
        TimeSlot slot = shift.getSlot();
        String dayLabel = slot != null && slot.getDay() != null
                ? capitalize(slot.getDay().getDisplayName(TextStyle.FULL, Locale.ITALIAN))
                : "-";
        String startLabel = slot != null && slot.getStartTime() != null
                ? slot.getStartTime().toString()
                : "-";
        long durationMinutes = slot != null && slot.getDuration() != null
                ? slot.getDuration().toMinutes()
                : 0;

        return "  - %s (%s) | Visita: %s | Inizio: %s | Durata: %d minuti"
                .formatted(
                        shift.getDate(),
                        dayLabel,
                        safe(shift.getVisitTypeId()),
                        startLabel,
                        durationMinutes
                );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String capitalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ITALIAN) + trimmed.substring(1);
    }

    private String formatState(VisitState visit){
        if (visit == null) { return "non specificato";}

        return switch(visit) {
            case PROPOSTA -> "Proposta";
            case COMPLETA -> "Completa";
            case CONFERMATA -> "Confermata";
            case CANCELLATA -> "Cancellata";
            case EFFETTUATA ->  "Effettata";
        };
    }
}
