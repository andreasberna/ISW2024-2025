package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignedShiftTest {

    @Test
    void constructorValidatesArguments() {
        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, Duration.ofHours(1));

        assertThatThrownBy(() -> new AssignedShift(null, "visit", slot))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("data");
        assertThatThrownBy(() -> new AssignedShift(LocalDate.now(), " ", slot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificativo");
        assertThatThrownBy(() -> new AssignedShift(LocalDate.now(), "visit", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fascia");
    }
}