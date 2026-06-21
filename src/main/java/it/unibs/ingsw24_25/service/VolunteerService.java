package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.AssignedShiftDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VolunteerService extends LoginService{

    @Transactional
    void submitAvailability(String nickname, MonthlyAvailability availability, LocalDate date);

    Optional<MonthlyAvailability> loadAvailability(String nickname, YearMonth month);

    List<AssignedShift> loadSchedule(String nickname, YearMonth month);

    @Transactional
    void assignShifts(String nickname, YearMonth month, List<AssignedShift> shifts);

    Map<String, MonthlyAvailability> snapshotAvailabilities(YearMonth month, LocalDate capturedOn);

    @Transactional
    void removeVolunteerAccount(String nickname);

    List<VisitOccurrenceDTO> loadConfirmedGuidedVisits(String nickname, YearMonth month);

    @Transactional
    void changePassword(String username, String oldPassword, String newPassword);

    List<AssignedShiftDTO> getAssignedShifts(String username);
}
