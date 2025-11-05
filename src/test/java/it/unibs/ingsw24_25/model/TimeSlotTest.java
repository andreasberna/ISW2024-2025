package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    @Test
    void constructorRejectsNullArguments() {
        assertThatThrownBy(() -> new TimeSlot(null, LocalTime.NOON, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("giorno");
        assertThatThrownBy(() -> new TimeSlot(DayOfWeek.MONDAY, null, Duration.ofHours(1)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("inizio");
        assertThatThrownBy(() -> new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("durata");
    }

    @Test
    void constructorRejectsNonPositiveDuration() {
        assertThatThrownBy(() -> new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positiva");
        assertThatThrownBy(() -> new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, Duration.ofMinutes(-30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void settersEnforceValidation() {
        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, Duration.ofMinutes(30));

        assertThatThrownBy(() -> slot.setDay(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> slot.setStartTime(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> slot.setDuration(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        slot.setDuration(Duration.ofMinutes(45));
        assertThat(slot.getDuration()).isEqualTo(Duration.ofMinutes(45));
    }
}