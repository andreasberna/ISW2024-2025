package it.unibs.ingsw24_25.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

// MOCK: This interface should be implemented using Spring Data JPA 
// with a proper Entity representing ProvisionedCredentials.
// For now, we will create a dummy implementation to satisfy Spring context.
@Repository
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
