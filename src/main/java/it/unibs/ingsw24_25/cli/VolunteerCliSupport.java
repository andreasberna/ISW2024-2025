package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.TimeSlot;
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
    private static final String VOLUNTEER_BOOKING_SUMMARY = "  %d) %s | %s | Orario: %s | Prenotazioni: %d | Codici: %s";
    private static final String VOLUNTEER_BOOKING_DETAIL_HEADER = "Dettaglio prenotazioni per %s (%s):";
    private static final String VOLUNTEER_BOOKING_DETAIL_NO_ITEMS = "  Nessuna prenotazione registrata.";

    private VolunteerCliSupport() {
    }

    static void displayAvailability(Printer printer,
                                    String nickname,
                                    YearMonth month,
                                    DateTimeFormatter formatter,
                                    Optional<MonthlyAvailability> availability) {
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(nickname, "Nickname non può essere nullo");
        Objects.requireNonNull(month, "Il mese non può essere nullo");
        Objects.requireNonNull(availability, "availability non può essere nullo");

        DateTimeFormatter effectiveFormatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy-MM") : formatter;

        availability.ifPresentOrElse (
                value -> printAvailability (printer, nickname, value),
                () -> printer.println (VOLUNTEER_NO_AVAILABILITY.formatted (nickname, month.format (effectiveFormatter)))
        );
    }

    static void displaySchedule(Printer printer,
                                String nickname,
                                YearMonth month,
                                DateTimeFormatter formatter,
                                List<AssignedShift> shifts) {
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(nickname, "Nickname non può essere nullo");
        Objects.requireNonNull(month, "Il mese non può essere nullo");
        Objects.requireNonNull (shifts, "Shifts non può essere nullo");

        DateTimeFormatter effectiveFormatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy-MM") : formatter;

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

    static void displayConfirmedVisits(Printer printer,
                                       String nickname,
                                       YearMonth month,
                                       DateTimeFormatter formatter,
                                       List<VisitOccurrenceDTO> visits) {
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(nickname, "Nickname non può essere nullo");
        Objects.requireNonNull(month, "Il mese non può essere nullo");
        Objects.requireNonNull(visits, "visits non può essere nullo");

        DateTimeFormatter effectiveFormatter = formatter == null ? DateTimeFormatter.ofPattern("yyyy-MM") : formatter;

        List<VisitOccurrenceDTO> sortedVisits = visits.stream ()
                        .filter (Objects::nonNull)
                        .sorted (Comparator.comparing (VisitOccurrenceDTO::getDate)
                                .thenComparing (VisitOccurrenceDTO::getStartTime, Comparator.nullsLast (Comparator.naturalOrder ()))
                                .thenComparing (VisitOccurrenceDTO::getTitle, Comparator.nullsLast (String::compareToIgnoreCase)))
                        .toList ();

        if (sortedVisits.isEmpty()) {
            printer.println (VOLUNTEER_NO_CONFIRMED.formatted(nickname, month.format(effectiveFormatter)));
            return;
        }

        printer.println (VOLUNTEER_CONFIRMED_HEADER.formatted(nickname, month.format(effectiveFormatter)));

        int index = 1;
        for (VisitOccurrenceDTO visit : sortedVisits) {
            printer.println (formatConfirmedVisitSummary (index++, visit));
        }
    }

    static void displayVisitBookings(Printer printer, VisitOccurrenceDTO visit) {
        Objects.requireNonNull(printer, "Printer non può essere nullo");
        Objects.requireNonNull(visit, "Visit non può essere nullo");

        String title = visit.getTitle() == null || visit.getTitle ().isBlank () ? visit.getVisitTypeId () : visit.getTitle ();
        printer.println (VOLUNTEER_BOOKING_DETAIL_HEADER.formatted(title, visit.getDate ()));

        List<VisitBookingDTO> bookings = visit.getBookings () == null ? List.of () : visit.getBookings ();
        if (bookings.isEmpty()) {
            printer.println (VOLUNTEER_BOOKING_DETAIL_NO_ITEMS);
            return;
        }

        bookings.stream()
                .filter (Objects::nonNull)
                .forEach (booking -> printer.println (formatBookingDetailLine(booking)));
    }

    private static String formatBookingDetailLine(VisitBookingDTO booking) {
        String code = booking.getCode () == null ? "" : booking.getCode ().trim ();
        String beneficiary = booking.getBeneficiaryName () == null || booking.getBeneficiaryName ().isBlank ()
                ? "-"
                : booking.getBeneficiaryName ().trim ();
        return " - Codice: %s | Prenotante: %s | Iscritti: %d".formatted (code, beneficiary, booking.getParticipants ());
    }

    private static String formatConfirmedVisitSummary(int index, VisitOccurrenceDTO visit) {
        if (visit == null) {return "";}

        String time = visit.getStartTime () == null ? "-" : visit.getStartTime ().toString ();
        List<VisitBookingDTO> bookings = visit.getBookings () == null ? List.of () : visit.getBookings ();
        String codes = bookings.stream ()
                .filter (Objects::nonNull)
                .map (VisitBookingDTO::getCode)
                .filter (Objects::nonNull)
                .map (String::trim)
                .filter (code -> !code.isEmpty ())
                .distinct ()
                .sorted (String::compareToIgnoreCase)
                .reduce ((left, right) -> left + ", " + right)
                .orElse ("-");

        return VOLUNTEER_BOOKING_SUMMARY.formatted (
                index,
                visit.getDate (),
                visit.getTitle (),
                time,
                bookings.size (),
                codes
        );
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
