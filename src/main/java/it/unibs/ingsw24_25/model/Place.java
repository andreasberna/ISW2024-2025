package it.unibs.ingsw24_25.model;

import java.util.ArrayList;
import java.util.List;

public class Place {
    private String placeTitle;
    private String placeDescription;
    private String location;
    private List<VisitType> visits = new ArrayList<VisitType> ();

    public Place(String placeTitle, String placeDescription, String location) {
        this.placeTitle = placeTitle;
        this.placeDescription = placeDescription;
        this.location = location;
    }

    public String getPlaceTitle() {
        return placeTitle;
    }
    public void setPlaceTitle(String placeTitle) {
        this.placeTitle = placeTitle;
    }
    public String getPlaceDescription() {
        return placeDescription;
    }
    public void setPlaceDescription(String placeDescription) {
        this.placeDescription = placeDescription;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public List<VisitType> getVisits() {
        return ensureVisitsInitialize();
    }

    private List<VisitType> ensureVisitsInitialize() {
        if(this.visits == null) this.visits = new ArrayList<>();
        return this.visits;
    }

    public void setVisits(List<VisitType> visits) {
        if(visits == null) this.visits = new ArrayList<>();
        else this.visits = new ArrayList<>(visits);
    }

    public void addVisit(VisitType visit){
        if(visit != null) ensureVisitsInitialize().add(visit);
    }
    public void removeVisit(VisitType visit){
        if(visit != null) ensureVisitsInitialize().remove(visit);
    }
}
