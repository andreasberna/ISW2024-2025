package it.unibs.ingsw24_25.repository;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProvisionedCredentialsRepositoryImpl implements ProvisionedCredentialsRepository {

    private final Map<String, String> configuratorCredentials = new HashMap<>();
    private final Map<String, String> volunteerCredentials = new HashMap<>();

    @Override
    public Optional<String> findConfiguratorPassword(String nickname) {
        return Optional.ofNullable(configuratorCredentials.get(nickname));
    }

    @Override
    public Optional<String> findVolunteerPassword(String nickname) {
        return Optional.ofNullable(volunteerCredentials.get(nickname));
    }

    @Override
    public void registerConfiguratorCredential(String nickname, String password) {
        configuratorCredentials.put(nickname, password);
    }

    @Override
    public void registerVolunteerCredential(String nickname, String password) {
        volunteerCredentials.put(nickname, password);
    }

    @Override
    public void consumeConfiguratorCredential(String nickname) {
        configuratorCredentials.remove(nickname);
    }

    @Override
    public void consumeVolunteerCredential(String nickname) {
        volunteerCredentials.remove(nickname);
    }

    @Override
    public boolean hasConfiguratorCredential(String nickname) {
        return configuratorCredentials.containsKey(nickname);
    }

    @Override
    public boolean hasVolunteerCredential(String nickname) {
        return volunteerCredentials.containsKey(nickname);
    }

    @Override
    public boolean hasAnyConfiguratorCredential() {
        return !configuratorCredentials.isEmpty();
    }
}