package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.Configurator;
import it.unibs.ingsw24_25.repository.ConfiguratorRepository;
import it.unibs.ingsw24_25.repository.ProvisionedCredentialsRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ConfiguratorCredentialManager {
    private final ConfiguratorRepository configuratorRepository;
    private final ProvisionedCredentialsRepository provisionedCredentialsRepository;
    private final Set<String> defaultCredentialsValidated = new HashSet<>();

    public ConfiguratorCredentialManager(ConfiguratorRepository configuratorRepository,
                                         ProvisionedCredentialsRepository provisionedCredentialsRepository) {
        this.configuratorRepository = Objects.requireNonNull(configuratorRepository, "Il repository dei configuratori non può essere nullo");
        this.provisionedCredentialsRepository = Objects.requireNonNull(provisionedCredentialsRepository, "Il repository delle credenziali provisionate non può essere nullo");
    }

    public boolean isFirstAccessPending(String nickname) {
        String sanitized = sanitizeNickname(nickname);
        if (sanitized == null) {
            return false;
        }
        return provisionedCredentialsRepository.hasConfiguratorCredential(sanitized);
    }

    public void verifyDefaultCredentials(String nickname, String password) {
        String sanitizedNickname = sanitizeNickname(nickname);
        if (sanitizedNickname == null) {
            throw new IllegalArgumentException("Il nickname di default non può essere vuoto");
        }
        String effectiveNickname = resolvePendingConfiguratorNickname(sanitizedNickname)
                .orElseThrow(() -> new IllegalStateException("Le credenziali personali sono già state impostate"));

        String sanitizedPassword = requireNonBlank(password, "La password di default non può essere vuota");
        String expectedPassword = provisionedCredentialsRepository.findConfiguratorPassword(effectiveNickname)
                .orElseThrow(() -> new IllegalStateException("Credenziali di primo accesso non registrate"));

        if (!expectedPassword.equals(sanitizedPassword)) {
            throw new IllegalArgumentException("Credenziali di primo accesso non valide");
        }

        defaultCredentialsValidated.add(effectiveNickname);
    }

    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        String sanitizedDefault = sanitizeNickname(currentNickname);
        if (sanitizedDefault == null) {
            throw new IllegalArgumentException("Il nickname di default non può essere vuoto");
        }
        if (!isFirstAccessPending(sanitizedDefault)) {
            throw new IllegalStateException("Le credenziali sono già state configurate");
        }
        if (!defaultCredentialsValidated.contains(sanitizedDefault)) {
            throw new IllegalStateException("Credenziali di default non ancora verificate");
        }

        String sanitizedNickname = requireNonBlank(newNickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");

        if (isConfiguratorNicknameTaken(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        boolean matchesDefaultNickname = sanitizedNickname.equalsIgnoreCase(sanitizedDefault);
        if (!matchesDefaultNickname && provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        configuratorRepository.save(new Configurator(sanitizedNickname, sanitizedPassword));
        provisionedCredentialsRepository.consumeConfiguratorCredential(sanitizedDefault);
        defaultCredentialsValidated.remove(sanitizedDefault);
    }

    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) {
            return false;
        }
        if (isFirstAccessPending(nickname)) {
            return false;
        }

        String normalizedNickname = nickname.trim();
        String normalizedPassword = password.trim();

        if (normalizedNickname.isEmpty() || normalizedPassword.isEmpty()) {
            return false;
        }

        return configuratorRepository.load()
                .map(map -> map.get(normalizedNickname))
                .map(configurator ->
                        Objects.equals(configurator.getPassword(), normalizedPassword))
                .orElse(false);
    }

    public void registerConfigurator(String nickname, String password) {
        String sanitizedNickname = requireNonBlank(nickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");

        Map<String, Configurator> existing = configuratorRepository.load()
                .map(HashMap::new)
                .orElseGet(HashMap::new);

        boolean duplicate = existing.keySet().stream()
                .filter(Objects::nonNull)
                .anyMatch(registered -> registered.equalsIgnoreCase(sanitizedNickname));

        if (duplicate) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        if (provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        configuratorRepository.save(new Configurator(sanitizedNickname, sanitizedPassword));
    }

    public List<String> listConfigurators() {
        return configuratorRepository.load()
                .map(map -> map.values().stream()
                        .filter(Objects::nonNull)
                        .map(Configurator::getNickname)
                        .filter(Objects::nonNull)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList())
                .orElseGet(List::of);
    }

    public boolean hasPendingConfiguratorSeeds() {
        return provisionedCredentialsRepository.hasAnyConfiguratorCredential();
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private Optional<String> resolvePendingConfiguratorNickname(String sanitizedNickname) {
        if (sanitizedNickname == null) {
            return Optional.empty();
        }

        if (provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            return Optional.of(sanitizedNickname);
        }

        if (sanitizedNickname.length() <= 1) {
            return Optional.empty();
        }

        for (int index = 0; index < sanitizedNickname.length(); index++) {
            String candidate = sanitizedNickname.substring(0, index) + sanitizedNickname.substring(index + 1);
            if (candidate.isBlank()) {
                continue;
            }
            if (provisionedCredentialsRepository.hasConfiguratorCredential(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private String sanitizeNickname(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isConfiguratorNicknameTaken(String nickname) {
        return configuratorRepository.load()
                .map(map -> map.values().stream()
                        .filter(Objects::nonNull)
                        .map(Configurator::getNickname)
                        .filter(Objects::nonNull)
                        .anyMatch(existing -> existing.equalsIgnoreCase(nickname)))
                .orElse(false);
    }
}
