package it.unibs.ingsw24_25.service.dtos;

public record VolunteerSetupData(
        String defaultNickname,
        String defaultPassword,
        String newNickname,
        String newPassword
) {
}