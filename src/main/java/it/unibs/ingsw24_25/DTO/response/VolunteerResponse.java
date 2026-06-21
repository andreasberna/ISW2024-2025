package it.unibs.ingsw24_25.DTO.response;

public record VolunteerResponse(
        String nickname,
        boolean active,
        boolean firstAccessPending
) {}
