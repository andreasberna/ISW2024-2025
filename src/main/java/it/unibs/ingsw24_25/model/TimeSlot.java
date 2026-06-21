package it.unibs.ingsw24_25.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
public class TimeSlot {

    @Column(name = "`day`")
    private DayOfWeek day;
    private LocalTime startTime;
    private int duration;

    protected TimeSlot() {}

    public TimeSlot(DayOfWeek day, LocalTime startTime, int duration) {
        this.day = day;
        this.startTime = startTime;
        this.duration = duration;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getDuration() {
        return duration;
    }
}