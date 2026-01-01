package it.unibs.ingsw24_25.controller;

import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.service.VolunteerService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VolunteerController {
    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = Objects.requireNonNull(volunteerService, "volunteerService non può essere nullo");
    }

    public void submitAvailability(String nickname,
                                   YearMonth month,
                                   EnumSet<DayOfWeek> preferredDays,
                                   int weeklyFrequency,
                                   LocalDate submittedOn) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(month, "month non può essere nullo");
        Objects.requireNonNull(preferredDays, "preferredDays non può essere nullo");
        Objects.requireNonNull(submittedOn, "submittedOn non può essere nullo");

        MonthlyAvailability availability = new MonthlyAvailability(month, preferredDays, weeklyFrequency, submittedOn);
        volunteerService.submitAvailability(nickname, availability, submittedOn);
    }

    public List<VisitOccurrenceDTO> loadConfirmedVisits(String nickname, YearMonth month) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(month, "month non può essere nullo");

        return volunteerService.loadConfirmedGuidedVisits(nickname, month);
    }

    public Optional<MonthlyAvailability> loadAvailability(String nickname, YearMonth month) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(month, "month non può essere nullo");

        return volunteerService.loadAvailability(nickname, month);
    }

    public List<AssignedShift> loadSchedule(String nickname, YearMonth month) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(month, "month non può essere nullo");

        return volunteerService.loadSchedule(nickname, month);
    }

    public boolean isFirstAccessPending(String nickname) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        return volunteerService.isFirstAccessPending(nickname);
    }

    public boolean verifyLogin(String nickname, String password) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(password, "password non può essere nullo");
        return volunteerService.verifyLogin(nickname, password);
    }

    public void verifyDefaultCredentials(String nickname, String password) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(password, "password non può essere nullo");
        volunteerService.verifyDefaultCredentials(nickname, password);
    }

    public void setPersonalCredentials(String currentNickname, String newNickname, String newPassword) {
        Objects.requireNonNull (currentNickname, "currentNickname non può essere nullo");
        Objects.requireNonNull (newNickname, "newNickname non può essere nullo");
        Objects.requireNonNull (newPassword, "newPassword non può essere nullo");
        volunteerService.setPersonalCredentials (currentNickname, newNickname, newPassword);
    }
}
