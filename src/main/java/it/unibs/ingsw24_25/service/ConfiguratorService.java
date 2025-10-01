package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.TimeSlot;

import java.time.LocalDate;
import java.util.List;

public interface ConfiguratorService {

    public void setTerritorialScope(String scope);
    public void setMaxPeoplePerSubscription(int max);
    public String addPlace(String name, String description, String location);
    public String addVisitType(String placeID, String title, String description, String meetLocation, List<TimeSlot> schedules,
                               boolean ticketRequired, int minParticipants, int maxParticipants, LocalDate validFrom, LocalDate validTo);
    public void addVolunteer(String nickname);
    public void linkVOlunteerToVisit(String nickname, String visitTypeId);
    public List<PlaceDTO> listPlace();
    public List<VisitTypeDTO> listVisitTypeByPlace(String placeId);
    public List<VolunteerDTO> listVolunteerWVisitType();

}
