package it.unibs.ingsw24_25.DTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class PlanAvailabilityDTO {

    private String volunteerNickname;
    private YearMonth referenceMonth;
    private List<DayOfWeek> preferredDays;
    private int weeklyFrequency;
    private LocalDate submittedOn;
    private LocalDate snapshotCapturedOn;

    public PlanAvailabilityDTO(String volunteerNickname,
                               YearMonth referenceMonth,
                               List<DayOfWeek> preferredDays,
                               int weeklyFrequency,
                               LocalDate submittedOn,
                               LocalDate snapshotCapturedOn) {
        this.volunteerNickname = Objects.requireNonNull(volunteerNickname, "volunteerNickname");
        this.referenceMonth = Objects.requireNonNull(referenceMonth, "referenceMonth");
        this.preferredDays = preferredDays;
        this.weeklyFrequency = weeklyFrequency;
        this.submittedOn = submittedOn;
        this.snapshotCapturedOn = snapshotCapturedOn;
    }

    public String getVolunteerNickname() {
        return volunteerNickname;
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

    public LocalDate getSnapshotCapturedOn() {
        return snapshotCapturedOn;
    }
}
