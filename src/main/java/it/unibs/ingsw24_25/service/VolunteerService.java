package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface VolunteerService extends LoginService{

    void submitAvailability(String nickname, MonthlyAvailability availability, LocalDate date);

    Optional<MonthlyAvailability> loadAvailability(String nickname, YearMonth month);

    List<AssignedShift> loadSchedule(String nickname, YearMonth month);

    void assignShifts(String nickname, YearMonth month, List<AssignedShift> shifts);

}
