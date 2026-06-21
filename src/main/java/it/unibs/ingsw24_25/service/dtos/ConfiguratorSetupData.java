package it.unibs.ingsw24_25.service.dtos;

public record ConfiguratorSetupData(
        String defaultNickname,
        String defaultPassword,
        String newNickname,
        String newPassword,
        String territorialScope,
        int maxParticipants
) {
}