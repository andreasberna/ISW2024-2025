package it.unibs.ingsw24_25.DTO;

import java.util.List;

public class VolunteerDTO {

    private String nickname;
    private String password;
    private boolean firstAccessPending;
    private List<String> visitTypeTitles;
    private List<VolunteerAvailabilityDTO> availabilities;
    private List<AssignedShiftDTO> scheduledShifts;

    public VolunteerDTO(String nickname,
                        String password,
                        boolean firstAccessPending,
                        List<String> visitTypeTitles,
                        List<VolunteerAvailabilityDTO> availabilities,
                        List<AssignedShiftDTO> scheduledShifts) {
        this.nickname = nickname;
        this.password = password;
        this.firstAccessPending = firstAccessPending;
        this.visitTypeTitles = visitTypeTitles;
        this.availabilities = availabilities;
        this.scheduledShifts = scheduledShifts;
    }

    public String getNickname(){
        return nickname;
    }
    public String getPassword(){
        return password;
    }
    public boolean isFirstAccessPending(){
        return firstAccessPending;
    }
    public List<String> getVisitTypeTitles(){
        return visitTypeTitles;
    }
    public List<VolunteerAvailabilityDTO> getAvailabilities(){
        return availabilities;
    }
    public List<AssignedShiftDTO> getScheduledShifts(){
        return scheduledShifts;
    }
}
