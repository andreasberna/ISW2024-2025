package it.unibs.ingsw24_25.DTO;

public class VisitTypeDTO {
    private String title;
    private String daySummary;
    private String startTime;
    private String endTime;
    private int durationMinutes;
    private boolean ticketRequired;
    private int minParticipants;
    private int maxParticipants;

    public VisitTypeDTO(String title, String daySummary, String startTime, String endTime,
                        int durationMinutes, boolean ticketRequired, int minParticipants, int maxParticipants) {
        this.title = title;
        this.daySummary = daySummary;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ticketRequired = ticketRequired;
        this.durationMinutes = durationMinutes;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
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
}
