package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.ProvisionedCredentialsRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class VolunteerCredentialManager {

    private final VolunteerRepository volunteerRepository;
    private final ProvisionedCredentialsRepository provisionedCredentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> defaultCredentialsValidated = new HashSet<>();

    public VolunteerCredentialManager(VolunteerRepository volunteerRepository,
                                      ProvisionedCredentialsRepository provisionedCredentialsRepository,
                                      PasswordEncoder passwordEncoder) {
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository);
        this.provisionedCredentialsRepository = Objects.requireNonNull(provisionedCredentialsRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Transactional
    public void removeVolunteerAccount(String nickname) {
        Volunteer volunteer = loadVolunteer(nickname);
        volunteer.deactivate();
        volunteerRepository.deleteByNickname(volunteer.getNickname());
        defaultCredentialsValidated.remove(volunteer.getNickname());
        provisionedCredentialsRepository.consumeVolunteerCredential(volunteer.getNickname());
    }

    public boolean isFirstAccessPending(String nickname) {
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.isFirstAccessPending();
    }

    @Transactional
    public void verifyDefaultCredentials(String nickname, String password) {
        Volunteer volunteer = loadVolunteer(nickname);
        if (!volunteer.isFirstAccessPending()) {
            throw new IllegalStateException("Le credenziali personali sono già state impostate");
        }
        String sanitizedPassword = requireNonBlank(password, "la Password di default non può essere nulla");
        String expectedPasswordHash = provisionedCredentialsRepository.findVolunteerPassword(volunteer.getNickname())
                .orElseThrow(() -> new IllegalStateException("Credenziali di primo accesso non registrate"));
                
        if (!passwordEncoder.matches(sanitizedPassword, expectedPasswordHash)) {
            throw new IllegalArgumentException("Credenziali di primo accesso non valide");
        }

        defaultCredentialsValidated.add(volunteer.getNickname());
    }

    @Transactional
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        Volunteer volunteer = loadVolunteer(currentNickname);
        if (!volunteer.isFirstAccessPending()) {
            throw new IllegalStateException("Le credenziali personali sono state impostate");
        }

        String sanitizedPassword = requireNonBlank(password, "La nuova password non può essere nulla");
        String sanitizedNickname = requireNonBlank(newNickname, "Il nuovo nickname non può essere vuoto");

        String current = volunteer.getNickname();
        if (current.equalsIgnoreCase(sanitizedNickname)) {
            throw new IllegalArgumentException("Il nuovo nickname deve essere diverso da quello assegnato");
        }

        if (volunteer.passwordMatches(sanitizedPassword, passwordEncoder)) {
            throw new IllegalArgumentException("La nuova password deve essere diversa da quella assegnata");
        }

        if (!current.equalsIgnoreCase(sanitizedNickname) && volunteerRepository.findByNickname(sanitizedNickname).isPresent()) {
            throw new IllegalArgumentException("Nickname già presente");
        }

        if (!defaultCredentialsValidated.contains(current)) {
            throw new IllegalStateException("Credenziali di default non ancora verificate");
        }

        volunteerRepository.deleteByNickname(current);
        volunteer.setNickname(sanitizedNickname);
        volunteer.setPersonalCredentials(passwordEncoder.encode(sanitizedPassword));
        volunteerRepository.save(volunteer);
        provisionedCredentialsRepository.consumeVolunteerCredential(current);
        defaultCredentialsValidated.remove(current);
    }

    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) {
            return false;
        }
        String sanitizedNick = nickname.trim();
        String sanitizedPassword = password.trim();
        if (sanitizedNick.isEmpty() || sanitizedPassword.isEmpty()) {
            return false;
        }

        Optional<Volunteer> volunteer = volunteerRepository.findByNickname(sanitizedNick);
        if (volunteer.isEmpty()) {
            return false;
        }

        Volunteer loaded = volunteer.get();
        if (loaded.isFirstAccessPending()) {
            return false;
        }

        return loaded.passwordMatches(sanitizedPassword, passwordEncoder);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Volunteer volunteer = loadVolunteer(username);

        if (!volunteer.passwordMatches(oldPassword, passwordEncoder)) {
            throw new IllegalArgumentException("Vecchia password errata");
        }

        volunteer.setPersonalCredentials(passwordEncoder.encode(newPassword));
        volunteerRepository.save(volunteer);
    }

    private Volunteer loadVolunteer(String nickname) {
        String sanitized = requireNonBlank(nickname, "Il nickname del volontario non può essere vuoto");
        return volunteerRepository.findByNickname(sanitized)
                .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + sanitized));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}