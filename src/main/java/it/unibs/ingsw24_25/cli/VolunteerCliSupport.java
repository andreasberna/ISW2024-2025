package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.service.VolunteerService;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class VolunteerCliSupport {
    private static final String VOLUNTEER_AVAILABILITY_HEADER = "Disponibilità registrata per %s nel mese %s:";
    private static final String VOLUNTEER_NO_AVAILABILITY = "Nessuna disponibilità registrata per %s nel mese %s.";
    private static final String VOLUNTEER_SCHEDULE_HEADER = "Turni assegnati a %s nel mese %s:";
    private static final String VOLUNTEER_NO_SHIFTS = "Nessun turno assegnato a %s per il mese %s.";
    private static final String VOLUNTEER_CONFIRMED_HEADER = "Visite confermate per %s nel mese %s:";
    private static final String VOLUNTEER_NO_CONFIRMED = "Nessuna visita confermata per %s nel mese %s.";

    private VolunteerCliSupport() {
    }

    static void displayAvailability(VolunteerService service,
                                    Printer printer,
                                    String nickname,
                                    YearMonth month,
                                    DateTimeFormatter formatter) {
        Objects.requireNonNull(service, "VolunteerService non può essere nullo");
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(nickname, "Nickname non può essere nullo");
        Objects.requireNonNull(month, "Il mese non può essere nullo");

        DateTimeFormatter effectiveFormatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy-MM") : formatter;

        service.loadAvailability(nickname, month)
                .ifPresentOrElse(
                        availability -> printAvailability(printer, nickname, availability),
                        () -> printer.println(VOLUNTEER_NO_AVAILABILITY.formatted(nickname, month.format(effectiveFormatter)))
                );
    }

    static void displaySchedule(VolunteerService service,
                                Printer printer,
                                String nickname,
                                YearMonth month,
                                DateTimeFormatter formatter) {
        Objects.requireNonNull(service, "VolunteerService non può essere nullo");
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(nickname, "Nickname non può essere nullo");
        Objects.requireNonNull(month, "Il mese non può essere nullo");

        DateTimeFormatter effectiveFormatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy-MM") : formatter;

        List<AssignedShift> shifts = service.loadSchedule(nickname, month);
        if (shifts.isEmpty()) {
            printer.println(VOLUNTEER_NO_SHIFTS.formatted(nickname, month.format(effectiveFormatter)));
            return;
        }

        printer.println(VOLUNTEER_SCHEDULE_HEADER.formatted(nickname, month.format(effectiveFormatter)));
        shifts.stream()
                .sorted((left, right) -> {
                    int dateComparison = left.getDate().compareTo(right.getDate());
                    if (dateComparison != 0) {
                        return dateComparison;
                    }
                    return left.getVisitTypeId().compareTo(right.getVisitTypeId());
                })
                .map(VolunteerCliSupport::formatShift)
                .forEach(printer::println);
    }

    static void displayConfirmedVisits(VolunteerService service,
                                       Printer printer,
                                       String nickname,
                                       YearMonth month,
                                       DateTimeFormatter formatter) {
        Objects.requireNonNull(service, "Service non può essere nullo");
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(nickname, "Nickname non può essere nullo");
        Objects.requireNonNull(month, "Il mese non può essere nullo");

        DateTimeFormatter effectiveFormatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy-MM") : formatter;

        List<VisitOccurrenceDTO> visits = service.loadConfirmedGuidedVisits (nickname, month);
        if (visits.isEmpty()) {
            printer.println (VOLUNTEER_NO_CONFIRMED.formatted(nickname, month.format(effectiveFormatter)));
            return;
        }

        printer.println (VOLUNTEER_CONFIRMED_HEADER.formatted(nickname, month.format(effectiveFormatter)));
        visits.stream()
                .sorted(Comparator.comparing(VisitOccurrenceDTO::getDate)
                        .thenComparing(VisitOccurrenceDTO::getStartTime, Comparator.nullsLast (Comparator.naturalOrder ())))
                .map (VolunteerCliSupport::formatConfirmedVisit)
                .forEach(printer::println);
    }

    private static String formatConfirmedVisit(VisitOccurrenceDTO visit) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ").append(visit.getDate()).append(" | ").append(visit.getTitle()).append(System.lineSeparator());
        builder.append("  Orario: ").append(visit.getStartTime() == null ? "-" : visit.getStartTime()).append(System.lineSeparator());
        builder.append("  Ritrovo: ").append(visit.getMeetingPoint()).append(System.lineSeparator());
        builder.append("  Partecipanti: ").append(visit.getBookedParticipants()).append(" / ").append(visit.getMaxParticipants()).append(System.lineSeparator());
        if (!visit.getBookings().isEmpty()) {
            builder.append("  Prenotazioni:").append(System.lineSeparator());
            for (VisitBookingDTO booking : visit.getBookings()) {
                builder.append("    * ").append(booking.getBeneficiaryName()).append(" - ")
                        .append(booking.getParticipants()).append(" partecipanti");
                if (booking.getNotes() != null && !booking.getNotes().isBlank()) {
                    builder.append(" | Note: ").append(booking.getNotes());
                }
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private static void printAvailability(Printer printer, String nickname, MonthlyAvailability availability) {
        printer.println(VOLUNTEER_AVAILABILITY_HEADER.formatted(nickname, availability.getReferenceMonth()));
        printer.println("  Giorni preferiti: " + formatDays(availability.getPreferredDays ()));
        printer.println("  Frequenza settimanale: " + availability.getWeeklyFrequency());
        printer.println("  Inviata il: " + availability.getSubmittedOn());
    }

    private static String formatShift(AssignedShift shift) {
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
                        shift.getVisitTypeId() == null ? "" : shift.getVisitTypeId(),
                        startLabel,
                        durationMinutes
                );
    }

    private static String formatDays(Iterable<DayOfWeek> days) {
        if (days == null) {
            return "-";
        }
        List<DayOfWeek> collected = new ArrayList<> ();
        for (DayOfWeek day : days) {
            collected.add(day);
        }
        if (collected.isEmpty()) {
            return "-";
        }

        return collected.stream()
                .sorted()
                .map(day -> capitalize(day.getDisplayName(TextStyle.FULL, Locale.ITALIAN)))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private static String capitalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ITALIAN) + trimmed.substring(1);
    }
}
