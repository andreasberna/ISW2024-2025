package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "places")
public class Place {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String placeTitle;

    @Lob
    private String placeDescription;
    private String location;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitType> visits = new ArrayList<>();

    protected Place() {}

    public Place(String placeTitle, String placeDescription, String location) {
        this.id = UUID.randomUUID().toString();
        this.placeTitle = requireNonBlank(placeTitle, "Il titolo del luogo non può essere nullo");
        this.placeDescription = sanitizeNullable(placeDescription);
        this.location = sanitizeNullable(location);
    }

    public String getId() {
        return id;
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
        return visits;
    }

    public void setVisits(List<VisitType> visits) {
        this.visits.clear();
        if (visits != null) {
            this.visits.addAll(visits);
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Objects.equals(id, place.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
