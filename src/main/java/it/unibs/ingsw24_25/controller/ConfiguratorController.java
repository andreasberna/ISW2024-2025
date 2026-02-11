package it.unibs.ingsw24_25.controller;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.service.ConfiguratorService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class ConfiguratorController {
    private final ConfiguratorService configuratorService;

    public ConfiguratorController(ConfiguratorService configuratorService) {
        this.configuratorService = Objects.requireNonNull(configuratorService, "configuratorService non può essere nullo");
    }

    public boolean hasPendingConfiguratorSeeds() {
        return configuratorService.hasPendingConfiguratorSeeds();
    }

    public List<String> listConfigurators() {
        return configuratorService.listConfigurators();
    }

    public boolean verifyLogin(String nickname, String password) {
        Objects.requireNonNull(nickname, "nickname non può essere nullo");
        Objects.requireNonNull(password, "password non può essere nullo");
        return configuratorService.verifyLogin(nickname, password);
    }

    public String addPlace(String title, String description, String location) {
        return configuratorService.addPlace(title, description, location);
    }

    public String addVisitType(String placeTitle,
                               String title,
                               String description,
                               String meetingPoint,
                               List<TimeSlot> slots,
                               boolean ticketRequired,
                               int minParticipants,
                               int maxParticipants,
                               LocalDate validFrom,
                               LocalDate validTo) {
        return configuratorService.addVisitType(placeTitle, title, description, meetingPoint, slots,
                ticketRequired, minParticipants, maxParticipants, validFrom, validTo);
    }

    public void addVolunteer(String nickname, String password) {
        configuratorService.addVolunteer(nickname, password);
    }

    public void linkVolunteerToVisit(String nickname, String visitId) {
        configuratorService.linkVOlunteerToVisit(nickname, visitId);
    }

    public List<PlaceDTO> listPlace() {
        return configuratorService.listPlace();
    }

    public List<VisitTypeDTO> listVisitTypeByPlace(String placeId) {
        return configuratorService.listVisitTypeByPlace(placeId);
    }
    public List<VisitOccurrenceDTO> listPlannedVisitsWithStatus() {
        return configuratorService.listPlannedVisitsWithStatus();
    }

    public List<VisitTypeDTO> listVisitType() {
        return configuratorService.listVisitType();
    }

    public List<VolunteerDTO> listVolunteerWithVisitType() {
        return configuratorService.listVolunteerWVisitType();
    }

    public void setBlackoutDates(List<LocalDate> dates) {
        configuratorService.setBlackoutDates(dates);
    }

    public void setMaxPeoplePerSubscription(int max) {
        configuratorService.setMaxPeoplePerSubscription(max);
    }

    public void closeAvailabilityWindow(LocalDate today) {
        configuratorService.closeAvailabilityWindow(today);
    }

    public MonthlyPlanDTO generateMonthlyPlan(YearMonth month) {
        return configuratorService.generateMonthlyPlan(month);
    }

    public void assignVolunteerToPlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId, String volunteerId) {
        configuratorService.assignVolunteerToPlannedVisit(month, date, slot, visitTypeId, volunteerId);
    }

    public void removePlannedVisit(YearMonth month, LocalDate date, TimeSlot slot, String visitTypeId) {
        configuratorService.removePlannedVisit(month, date, slot, visitTypeId);
    }

    public void removePlace(String placeId) {
        configuratorService.removePlace(placeId);
    }

    public void removeVisitType(String visitTypeId) {
        configuratorService.removeVisitType(visitTypeId);
    }

    public void removeVolunteer(String volunteer) {
        configuratorService.removeVolunteer(volunteer);
    }

    public void reopenAvailabilityWindow(LocalDate today) {
        configuratorService.reopenAvailabilityWindow(today);
    }

    public MonthlyPlanDTO getMonthlyPlanDetails(YearMonth month) {
        return configuratorService.getMonthlyPlanDetails(month);
    }

    public void verifyDefaultCredentials(String nickname, String password) {
        configuratorService.verifyDefaultCredentials(nickname, password);
    }

    public void setPersonalCredentials(String defaultNickname, String nickname, String password) {
        configuratorService.setPersonalCredentials(defaultNickname, nickname, password);
    }

    public void setTerritorialScope(String scope) {
        configuratorService.setTerritorialScope(scope);
    }
}
