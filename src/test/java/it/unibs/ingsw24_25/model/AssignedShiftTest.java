package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignedShiftTest {

    @Test
    void constructorInitializesFields() {
        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, 60);
        Volunteer volunteer = new Volunteer("alice", "password");
        YearMonth month = YearMonth.of(2024, 1);
        LocalDate date = LocalDate.of(2024, 1, 1);

        AssignedShift shift = new AssignedShift(volunteer, month, date, "visit", slot);

        assertThat(shift.getVolunteer()).isEqualTo(volunteer);
        assertThat(shift.getMonth()).isEqualTo(month);
        assertThat(shift.getDate()).isEqualTo(date);
        assertThat(shift.getVisitTypeId()).isEqualTo("visit");
        assertThat(shift.getSlot()).isEqualTo(slot);
    }
}