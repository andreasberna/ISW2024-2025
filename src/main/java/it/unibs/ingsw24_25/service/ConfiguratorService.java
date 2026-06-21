package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.DTO.response.MonthlyPlanResponseDTO;
import it.unibs.ingsw24_25.DTO.response.PlaceResponse;
import it.unibs.ingsw24_25.DTO.response.VisitTypeResponse;
import it.unibs.ingsw24_25.DTO.response.VolunteerResponse;
import it.unibs.ingsw24_25.model.PlanningPhase;
import it.unibs.ingsw24_25.model.TimeSlot;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ConfiguratorService extends LoginService {

    @Transactional
    SystemSettingsDTO getSystemSettings();

    @Transactional
    void defineTerritorialScope(String scope);

    @Transactional
    void setTerritorialScope(String scope);

    @Transactional
    void setMaxPeoplePerSubscription(int max);

    @Transactional
    String addPlace(String name, String description, String location);

    @Transactional
    String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules,
                        boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo);

    @Transactional
    void addVolunteer(String nickname, String defaultPassword);

    @Transactional
    void linkVolunteerToVisit(String nickname, String visitTypeId);

    List<PlaceResponse> listPlace();

    List<VisitTypeResponse> listVisitTypeByPlace(String placeId);

    List<VisitTypeResponse> listVisitType();

    List<VisitOccurrenceDTO> listPlannedVisitsWithStatus();

    List<VolunteerResponse> listVolunteerWVisitType();

    @Transactional
    void setBlackoutDates(List<LocalDate> excludedDates);

    @Transactional
    void closeAvailabilityWindow(LocalDate today);

    @Transactional
    MonthlyPlanDTO generateMonthlyPlan(YearMonth targetMonth);

    @Transactional
    void generateMonthlyPlanVisits(Long planId);

    MonthlyPlanDTO getMonthlyPlanDetails(YearMonth month);
    
    List<MonthlyPlanResponseDTO> listMonthlyPlans();

    @Transactional
    void publishMonthlyPlan(Long planId);

    @Transactional
    void updatePlanPhase(Long planId, PlanningPhase newPhase);

    @Transactional
    void assignVolunteerToPlannedVisit(YearMonth month, LocalDate date, TimeSlot slot,
                                       String visitTypeId, String volunteerNickname);

    @Transactional
    void removePlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId);

    @Transactional
    void removePlace(String placeID);

    @Transactional
    void removeVisitType(String visitTypeId);

    @Transactional
    void removeVolunteer(String nickname);

    @Transactional
    void reopenAvailabilityWindow(LocalDate today);

    List<String> listConfigurators();

    boolean hasPendingConfiguratorSeeds();

    @Transactional
    void registerConfigurator(String nickname, String password);

    @Transactional
    void changePassword(String username, String oldPassword, String newPassword);
}
