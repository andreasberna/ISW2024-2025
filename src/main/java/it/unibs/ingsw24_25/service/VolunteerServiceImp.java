package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.AssignedShiftDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import it.unibs.ingsw24_25.util.AvailabilitySubmissionPolicy;
import it.unibs.ingsw24_25.util.DTOMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VolunteerServiceImp implements VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final PasswordEncoder passwordEncoder;

    public VolunteerServiceImp(VolunteerRepository volunteerRepository, PasswordEncoder passwordEncoder) {
        this.volunteerRepository = volunteerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        if (nickname == null) return false;
        return volunteerRepository.findByNickname(nickname.trim())
                .map(Volunteer::isFirstAccessPending)
                .orElse(false);
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        // volunteers use direct password, not provisioned credentials
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        // volunteers keep the same nickname
        Volunteer volunteer = volunteerRepository.findByNickname(currentNickname)
                .orElseThrow(() -> new EntityNotFoundException("Volontario non trovato: " + currentNickname));
        volunteer.setPersonalCredentials(passwordEncoder.encode(password));
        volunteerRepository.save(volunteer);
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) return false;
        return volunteerRepository.findByNickname(nickname.trim())
                .map(v -> passwordEncoder.matches(password, v.getPassword()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void submitAvailability(String username, MonthlyAvailability availability, LocalDate date) {
        Volunteer volunteer = volunteerRepository.findByNickname(username)
                .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + username));

        if (availability.getMonth().isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("Il mese di disponibilità non può essere nel passato.");
        }

        volunteer.registerAvailability(availability);
        volunteerRepository.save(volunteer);
    }

    @Override
    public Optional<MonthlyAvailability> loadAvailability(String nickname, YearMonth month) {
        return Optional.empty();
    }

    @Override
    public List<AssignedShift> loadSchedule(String nickname, YearMonth month) {
        return List.of();
    }

    @Override
    public void assignShifts(String nickname, YearMonth month, List<AssignedShift> shifts) {

    }

    @Override
    public Map<String, MonthlyAvailability> snapshotAvailabilities(YearMonth month, LocalDate today) {
        return volunteerRepository.findAll().stream()
                .map(volunteer -> volunteer.findAvailability(month)
                        .map(availability -> new MonthlyAvailability(
                                availability.getMonth(),
                                availability.getPreferredDays(),
                                availability.getPreferredOccurrences(),
                                availability.getSubmittedOn(),
                                true, // Mark as snapshot
                                today
                        ))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(
                        availability -> availability.getVolunteer().getNickname(),
                        Function.identity()
                ));
    }

    @Override
    public void removeVolunteerAccount(String nickname) {

    }

    @Override
    public List<VisitOccurrenceDTO> loadConfirmedGuidedVisits(String nickname, YearMonth month) {
        return List.of();
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Volunteer volunteer = volunteerRepository.findByNickname(username)
                .orElseThrow(() -> new EntityNotFoundException("Volontario non trovato: " + username));
        if (!passwordEncoder.matches(oldPassword, volunteer.getPassword())) {
            throw new IllegalArgumentException("Vecchia password non corretta");
        }
        volunteer.setPersonalCredentials(passwordEncoder.encode(newPassword));
        volunteerRepository.save(volunteer);
    }

    @Override
    public List<AssignedShiftDTO> getAssignedShifts(String username) {
        Volunteer volunteer = volunteerRepository.findByNickname(username)
                .orElseThrow(() -> new IllegalArgumentException("Volontario non trovato: " + username));
        return volunteer.getScheduledShifts().stream()
                .map(DTOMapper::toAssignedShiftDTO)
                .collect(Collectors.toList());
    }
}
