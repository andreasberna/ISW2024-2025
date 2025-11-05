package it.unibs.ingsw24_25.repository;

import java.util.Optional;

public interface ProvisionedCredentialsRepository {
    Optional<String> findConfiguratorPassword(String nickname);

    Optional<String> findVolunteerPassword(String nickname);

    void registerConfiguratorCredential(String nickname, String password);

    void registerVolunteerCredential(String nickname, String password);

    void consumeConfiguratorCredential(String nickname);

    void consumeVolunteerCredential(String nickname);

    boolean hasConfiguratorCredential(String nickname);

    boolean hasVolunteerCredential(String nickname);

    boolean hasAnyConfiguratorCredential();
}
