package it.unibs.ingsw24_25.DTO;

import java.util.List;

public class VolunteerDTO {

    private String nickname;
    private List<String> visitTypeTitles;

    public VolunteerDTO(String nickname, List<String> visitTypeTitles) {
        this.nickname = nickname;
        this.visitTypeTitles = visitTypeTitles;
    }

    public String getNickname(){
        return nickname;
    }
    public List<String> getVisitTypeTitles(){
        return visitTypeTitles;
    }
}
