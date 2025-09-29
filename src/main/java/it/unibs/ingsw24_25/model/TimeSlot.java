package it.unibs.ingsw24_25.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

public class TimeSlot {

    private DayOfWeek day;
    private LocalTime startTime;
    private Duration duration;

    public TimeSlot(DayOfWeek day, LocalTime startTime, Duration duration) {
        this.day = day;
        this.startTime = startTime;
        this.duration = duration;
    }


    public DayOfWeek getDay() {
        return day;
    }
    public void setDay(DayOfWeek day) {
        this.day = day;
    }
    public LocalTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    public Duration getDuration() {
        return duration;
    }
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

}
