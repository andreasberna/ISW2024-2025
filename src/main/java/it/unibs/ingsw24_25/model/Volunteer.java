package it.unibs.ingsw24_25.model;

import java.util.ArrayList;
import java.util.List;

public class Volunteer {
    private String nickname;
    private List<VisitType> visitsAttending = new ArrayList<> ();

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
        return ensureVisitInitialized();
    }

    public void setVisitsAttending(List<VisitType> visitsAttending) {
        if (visitsAttending == null) this.visitsAttending = new ArrayList<> ();
        else  this.visitsAttending = new ArrayList<> (visitsAttending);
    }

    public void addVisit(VisitType visit){
        if(visit != null) ensureVisitInitialized ().add (visit);
    }
    public void removeVisit(VisitType visit){
        if (visit != null) ensureVisitInitialized ().remove (visit);
    }

    private List<VisitType> ensureVisitInitialized() {
        if(this.visitsAttending == null) this.visitsAttending = new ArrayList<> ();
        return this.visitsAttending;
    }
}
