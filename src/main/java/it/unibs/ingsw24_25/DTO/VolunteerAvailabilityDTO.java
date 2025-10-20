package it.unibs.ingsw24_25.DTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class VolunteerAvailabilityDTO {

    private YearMonth referenceMonth;
    private List<DayOfWeek> preferredDays;
    private int weeklyFrequency;
    private LocalDate submittedOn;

    public VolunteerAvailabilityDTO(YearMonth referenceMonth,
                                    List<DayOfWeek> preferredDays,
                                    int weeklyFrequency,
                                    LocalDate submittedOn) {
        this.referenceMonth = referenceMonth;
        this.preferredDays = preferredDays;
        this.weeklyFrequency = weeklyFrequency;
        this.submittedOn = submittedOn;
    }

    public YearMonth getReferenceMonth() {
        return referenceMonth;
    }
    public List<DayOfWeek> getPreferredDays() {
        return preferredDays;
    }
    public int getWeeklyFrequency() {
        return weeklyFrequency;
    }
    public LocalDate getSubmittedOn() {
        return submittedOn;
    }
}
