package it.unibs.ingsw24_25.DTO;

import it.unibs.ingsw24_25.model.Place;
import it.unibs.ingsw24_25.model.VisitState;
import it.unibs.ingsw24_25.model.Volunteer;

import java.util.List;

public class VisitTypeDTO {
    private String title;
    private String daySummary;
    private String startTime;
    private String endTime;
    private int durationMinutes;
    private boolean ticketRequired;
    private int minParticipants;
    private int maxParticipants;
    private String placeID;
    private VisitState state;


    public VisitTypeDTO(String title, String daySummary, String startTime, String endTime,
                        int durationMinutes, boolean ticketRequired, int minParticipants, int maxParticipants, String placeID, VisitState state ) {
        this.title = title;
        this.daySummary = daySummary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ticketRequired = ticketRequired;
        this.durationMinutes = durationMinutes;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.placeID = placeID;
        this.state = state;
    }

    public String getTitle() {
        return title;
    }
    public String getDaySummary() {
        return daySummary;
    }
    public String getStartTime() {
        return startTime;
    }
    public String getendTime(){
        return endTime;
    }
    public int getDurationMinutes() {
        return durationMinutes;
    }
    public boolean isTicketRequired() {
        return ticketRequired;
    }
    public int getMinParticipants() {
        return minParticipants;
    }
    public int getMaxParticipants() {
        return maxParticipants;
    }
    public String getPlaceID() {
        return placeID;
    }
    public VisitState getState() {
        return state;
    }
}
