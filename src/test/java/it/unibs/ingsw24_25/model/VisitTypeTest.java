package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisitTypeTest {

    @Test
    void generateOccurrencesCreatesVisitsForMatchingDays() {
        Place place = new Place("Museo", "desc", "Brescia");
        LocalDate validFrom = LocalDate.of(2024, 1, 10);
        LocalDate validTo = LocalDate.of(2024, 1, 20);
        TimeSlot slot1 = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, 60);
        TimeSlot slot2 = new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.NOON, 60);
        VisitType visitType = new VisitType("Title", "Desc", "Loc", validFrom, validTo,
                List.of(slot1, slot2), false, 1, 10, place);
        
        List<PlannedVisit> occurrences = visitType.generateOccurrences(YearMonth.of(2024, 1));

        assertThat(occurrences).hasSize(3);
        // Jan 10 is Wednesday, Jan 15 is Monday, Jan 17 is Wednesday
        assertThat(occurrences.get(0).getVisitDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(occurrences.get(1).getVisitDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(occurrences.get(2).getVisitDate()).isEqualTo(LocalDate.of(2024, 1, 17));
    }
}
