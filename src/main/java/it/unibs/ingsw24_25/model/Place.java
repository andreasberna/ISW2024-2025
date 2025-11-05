package it.unibs.ingsw24_25.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Place {
    private String placeTitle;
    private String placeDescription;
    private String location;
    private List<VisitType> visits = new ArrayList<VisitType> ();

    public Place(String placeTitle, String placeDescription, String location) {
        this.placeTitle = requireNonBlank(placeTitle, "Il titolo del luogo non può essere nullo");
        this.placeDescription = sanitizeNullable(placeDescription);
        this.location = sanitizeNullable(location);
    }

    public String getPlaceTitle() {
        return placeTitle;
    }
    public void setPlaceTitle(String placeTitle) {
        this.placeTitle = requireNonBlank(placeTitle, "Il titolo del luogo non può essere nullo");
    }
    public String getPlaceDescription() {
        return placeDescription;
    }
    public void setPlaceDescription(String placeDescription) {
        this.placeDescription = sanitizeNullable(placeDescription);
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = sanitizeNullable(location);
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

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String sanitizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
