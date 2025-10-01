package it.unibs.ingsw24_25.DTO;

public class PlaceDTO {
    private String title;
    private String description;
    private String location;

    public PlaceDTO(String title, String description, String location) {
        this.title = title;
        this.description = description;
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }
}
