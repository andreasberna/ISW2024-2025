package it.unibs.ingsw24_25.model;

import java.util.List;

public class Volunteer {
    private String nickname;
    private List<VisitType> visitsAttending;

    public Volunteer(String nickname){
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public List<VisitType> getVisitsAttending() {
        return visitsAttending;
    }
    public void setVisitsAttending(List<VisitType> visitsAttending) {
        this.visitsAttending = visitsAttending;
    }

    public void addVisit(VisitType visit){
        this.visitsAttending.add(visit);
    }
    public void removeVisit(VisitType visit){
        this.visitsAttending.remove(visit);
    }
}
