package it.unibs.ingsw24_25.model;

import java.util.List;

public class Place {
    private String placeTitle;
    private String placeDescription;
    private String location;
    private List<VisitType> visits;

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
        return visits;
    }
    public void setVisits(List<VisitType> visits) {
        this.visits = visits;
    }

    public void addVisit(VisitType visit){
        this.visits.add(visit);
    }
    public void removeVisit(VisitType visit){
        this.visits.remove(visit);
    }
}
