package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.VisitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VolunteerCliSupportTest {


    private ByteArrayOutputStream output;
    private Printer printer;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream ();
        printer = new Printer (new PrintStream (output));
    }

    @Test
    void displayConfirmedVisitsListsCodesAndReturnsSortedVisits() {
        YearMonth month = YearMonth.of (2024, 5);
        VisitOccurrenceDTO second = createVisit (
                "v-002",
                month,
                LocalDate.of (2024, 5, 12),
                LocalTime.of (10, 0),
                "Visita Castello",
                List.of (
                        createBooking ("B002", "Carla Bianchi", 3, LocalDate.of (2024, 5, 12), "Visita Castello"),
                        createBooking ("B001", "Mario Verdi", 2, LocalDate.of (2024, 5, 12), "Visita Castello")
                )
        );
        VisitOccurrenceDTO first = createVisit (
                "v-001",
                month,
                LocalDate.of (2024, 5, 4),
                LocalTime.of (9, 30),
                "Tour Museo",
                List.of (createBooking ("A100", "Luca Neri", 5, LocalDate.of (2024, 5, 4), "Tour Museo"))
        );

        VolunteerCliSupport.displayConfirmedVisits (
                printer,
                "guide01",
                month,
                DateTimeFormatter.ofPattern ("yyyy-MM"),
                List.of (second, first)
        );

        String rendered = output.toString ();
        int firstIndex = rendered.indexOf ("Tour Museo");
        int secondIndex = rendered.indexOf ("Visita Castello");
        assertThat (firstIndex).isPositive ();
        assertThat (secondIndex).isGreaterThan (firstIndex);
        assertThat (rendered).contains ("Codici: A100");
        assertThat (rendered).contains ("Codici: B001, B002");
    }

    @Test
    void displayVisitBookingsPrintsCodeNameAndParticipants() {
        YearMonth month = YearMonth.of (2024, 6);
        VisitOccurrenceDTO visit = createVisit (
                "v-010",
                month,
                LocalDate.of (2024, 6, 2),
                LocalTime.of (15, 0),
                "Passeggiata",
                List.of (
                        createBooking ("C300", "Anna Rosa", 4, LocalDate.of (2024, 6, 2), "Passeggiata"),
                        createBooking ("C301", "", 2, LocalDate.of (2024, 6, 2), "Passeggiata")
                )
        );

        VolunteerCliSupport.displayVisitBookings (printer, visit);

        String rendered = output.toString ();
        assertThat (rendered).contains ("Codice: C300");
        assertThat (rendered).contains ("Prenotante: Anna Rosa");
        assertThat (rendered).contains ("Iscritti: 4");
        assertThat (rendered).contains ("Codice: C301");
        assertThat (rendered).contains ("Prenotante: -");
    }

    @Test
    void displayVisitBookingsPrintsEmptyMessageWhenNoBookings() {
        YearMonth month = YearMonth.of (2024, 7);
        VisitOccurrenceDTO visit = createVisit (
                "v-100",
                month,
                LocalDate.of (2024, 7, 10),
                LocalTime.NOON,
                "Tour Giardino",
                List.of ()
        );

        VolunteerCliSupport.displayVisitBookings (printer, visit);

        assertThat (output.toString ()).contains ("Nessuna prenotazione registrata");
    }

    private VisitOccurrenceDTO createVisit(String id,
                                           YearMonth month,
                                           LocalDate date,
                                           LocalTime startTime,
                                           String title,
                                           List<VisitBookingDTO> bookings) {
        return new VisitOccurrenceDTO (
                id,
                month,
                date,
                startTime,
                title,
                "",
                "Piazza",
                false,
                1,
                20,
                bookings.stream ().mapToInt (VisitBookingDTO::getParticipants).sum (),
                VisitStatus.CONFIRMED,
                "type-" + id,
                bookings
        );
    }

    private VisitBookingDTO createBooking(String code,
                                          String beneficiary,
                                          int participants,
                                          LocalDate date,
                                          String title) {
        return new VisitBookingDTO (
                code,
                beneficiary,
                participants,
                "",
                date,
                title,
                VisitStatus.CONFIRMED
        );
    }
}