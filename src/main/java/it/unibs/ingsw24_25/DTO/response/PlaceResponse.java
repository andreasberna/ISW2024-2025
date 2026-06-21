package it.unibs.ingsw24_25.DTO.response;

public record PlaceResponse(
        String id,
        String placeTitle,
        String placeDescription,
        String location
) {}
