package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.TimeSlot;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ConfiguratorService {

    boolean isFirstAccessPending();

    void verifyDefaultCredentials(String nickname, String password);

    void setPersonalCredentials(String nickname, String password);

    boolean verifyLogin(String nickname, String password);

    void setTerritorialScope(String scope);

    void setMaxPeoplePerSubscription(int max);

    String addPlace(String name, String description, String location);

    String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules,

                        boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo);

    void addVolunteer(String nickname);

    void linkVOlunteerToVisit(String nickname, String visitTypeId);

    List<PlaceDTO> listPlace();

    List<VisitTypeDTO> listVisitTypeByPlace(String placeId);

    List<VisitTypeDTO> listVisitType();

    List<VolunteerDTO> listVolunteerWVisitType();

    void setBlackoutDates(List<LocalDate> excludedDates);


}
