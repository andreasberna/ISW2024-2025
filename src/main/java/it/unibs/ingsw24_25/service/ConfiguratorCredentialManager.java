package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.Configurator;
import it.unibs.ingsw24_25.repository.ConfiguratorRepository;
import it.unibs.ingsw24_25.repository.ProvisionedCredentialsRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfiguratorCredentialManager {
    private final ConfiguratorRepository configuratorRepository;
    private final ProvisionedCredentialsRepository provisionedCredentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> defaultCredentialsValidated = new HashSet<>();

    public ConfiguratorCredentialManager(ConfiguratorRepository configuratorRepository,
                                         ProvisionedCredentialsRepository provisionedCredentialsRepository,
                                         PasswordEncoder passwordEncoder) {
        this.configuratorRepository = Objects.requireNonNull(configuratorRepository, "Il repository dei configuratori non può essere nullo");
        this.provisionedCredentialsRepository = Objects.requireNonNull(provisionedCredentialsRepository, "Il repository delle credenziali provisionate non può essere nullo");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "Il password encoder non può essere nullo");
    }

    public boolean isFirstAccessPending(String nickname) {
        String sanitized = sanitizeNickname(nickname);
        if (sanitized == null) {
            return false;
        }
        return provisionedCredentialsRepository.hasConfiguratorCredential(sanitized);
    }

    @Transactional
    public void verifyDefaultCredentials(String nickname, String password) {
        String sanitizedNickname = sanitizeNickname(nickname);
        if (sanitizedNickname == null) {
            throw new IllegalArgumentException("Il nickname di default non può essere vuoto");
        }
        String effectiveNickname = resolvePendingConfiguratorNickname(sanitizedNickname)
                .orElseThrow(() -> new IllegalStateException("Le credenziali personali sono già state impostate"));

        String sanitizedPassword = requireNonBlank(password, "La password di default non può essere vuota");
        String expectedPasswordHash = provisionedCredentialsRepository.findConfiguratorPassword(effectiveNickname)
                .orElseThrow(() -> new IllegalStateException("Credenziali di primo accesso non registrate"));

        if (!passwordEncoder.matches(sanitizedPassword, expectedPasswordHash)) {
            throw new IllegalArgumentException("Credenziali di primo accesso non valide");
        }

        defaultCredentialsValidated.add(effectiveNickname);
    }

    @Transactional
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

        configuratorRepository.save(new Configurator(sanitizedNickname, passwordEncoder.encode(sanitizedPassword)));
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

        Optional<Configurator> configuratorOpt = configuratorRepository.findByNickname(normalizedNickname);

        return configuratorOpt
                .map(configurator -> passwordEncoder.matches(normalizedPassword, configurator.getPassword()))
                .orElse(false);
    }

    @Transactional
    public void registerConfigurator(String nickname, String password) {
        String sanitizedNickname = requireNonBlank(nickname, "Il nickname non può essere vuoto");
        String sanitizedPassword = requireNonBlank(password, "La password non può essere vuota");

        boolean duplicate = configuratorRepository.findByNickname(sanitizedNickname).isPresent();

        if (duplicate) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        if (provisionedCredentialsRepository.hasConfiguratorCredential(sanitizedNickname)) {
            throw new IllegalArgumentException("Esiste già un configuratore con questo nickname");
        }

        configuratorRepository.save(new Configurator(sanitizedNickname, passwordEncoder.encode(sanitizedPassword)));
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        String sanitizedNickname = requireNonBlank(username, "Il nickname non può essere vuoto");
        String sanitizedOldPassword = requireNonBlank(oldPassword, "La vecchia password non può essere vuota");
        String sanitizedNewPassword = requireNonBlank(newPassword, "La nuova password non può essere vuota");

        Configurator configurator = configuratorRepository.findByNickname(sanitizedNickname)
                .orElseThrow(() -> new IllegalArgumentException("Configuratore non trovato"));

        if (!passwordEncoder.matches(sanitizedOldPassword, configurator.getPassword())) {
            throw new IllegalArgumentException("Vecchia password errata");
        }

        configurator.setPassword(passwordEncoder.encode(sanitizedNewPassword));
        configuratorRepository.save(configurator);
    }

    public List<String> listConfigurators() {
        return configuratorRepository.findAll().stream()
                .map(Configurator::getNickname)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
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
        return configuratorRepository.findByNickname(nickname).isPresent();
    }
}