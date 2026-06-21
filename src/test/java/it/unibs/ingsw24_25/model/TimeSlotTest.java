package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSlotTest {

    @Test
    void constructorInitializesFields() {
        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, 60);

        assertThat(slot.getDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.NOON);
        assertThat(slot.getDuration()).isEqualTo(60);
    }
}