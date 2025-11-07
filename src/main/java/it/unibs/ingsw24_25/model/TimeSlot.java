package it.unibs.ingsw24_25.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public class TimeSlot {

    private DayOfWeek day;
    private LocalTime startTime;
    private Duration duration;

    public TimeSlot(DayOfWeek day, LocalTime startTime, Duration duration) {
        this.day = Objects.requireNonNull(day, "Il giorno non può essere nullo");
        this.startTime = Objects.requireNonNull(startTime, "L'orario di inizio non può essere nullo");
        this.duration = requirePositiveDuration(duration);
    }


    public DayOfWeek getDay() {
        return day;
    }
    public void setDay(DayOfWeek day) {
        this.day = Objects.requireNonNull(day,  "Il giorno non può essere nullo");
    }
    public LocalTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalTime startTime) {
        this.startTime = Objects.requireNonNull(startTime, "L'orario di inizio non può essere nulla");
    }
    public Duration getDuration() {
        return duration;
    }
    public void setDuration(Duration duration) {
        this.duration = requirePositiveDuration(duration);
    }

    private Duration requirePositiveDuration(Duration duration) {
        Objects.requireNonNull(duration, "La durata non può essere nulla");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("La durata deve essere positiva");
        }
        return duration;
    }
}
