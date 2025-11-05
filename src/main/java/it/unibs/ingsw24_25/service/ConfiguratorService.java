package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.MonthlyPlanDTO;
import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.TimeSlot;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ConfiguratorService extends LoginService {

    void setTerritorialScope(String scope);

    void setMaxPeoplePerSubscription(int max);

    String addPlace(String name, String description, String location);

    String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules,
                        boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo);

    void addVolunteer(String nickname, String defaultPassword);

    void linkVOlunteerToVisit(String nickname, String visitTypeId);

    List<PlaceDTO> listPlace();

    List<VisitTypeDTO> listVisitTypeByPlace(String placeId);

    List<VisitTypeDTO> listVisitType();

    List<VolunteerDTO> listVolunteerWVisitType();

    void setBlackoutDates(List<LocalDate> excludedDates);

    void closeAvailabilityWindow(LocalDate today);

    void applyCatalogChange(Runnable change);

    MonthlyPlanDTO generateMonthlyPlan(YearMonth targetMonth);

    MonthlyPlanDTO getMonthlyPlanDetails(YearMonth month);

    void assignVolunteerToPlannedVisit(YearMonth month, LocalDate date, TimeSlot slot,
                                       String visitTypeId, String volunteerNickname);

    void removePlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId);

    void removePlace(String placeID);

    void removeVisitType(String visitTypeId);

    void removeVolunteer(String nickname);

    void reopenAvailabilityWindow(LocalDate today);

    void registerConfigurator(String nickname, String password);

    List<String> listConfigurators();

    boolean hasPendingConfiguratorSeeds();
}
