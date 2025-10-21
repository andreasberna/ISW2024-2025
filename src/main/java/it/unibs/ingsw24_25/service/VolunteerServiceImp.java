package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.VolunteerRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class VolunteerServiceImp implements VolunteerService{

    private final VolunteerRepository volunteerRepository;
    private final Set<String> defaultCredentialsValidated = new HashSet<> ();

    public VolunteerServiceImp(VolunteerRepository volunteerRepository) {
        this.volunteerRepository = Objects.requireNonNull(volunteerRepository);
    }


    @Override
    public void submitAvailability(String nickname, MonthlyAvailability availability, LocalDate today) {
        Objects.requireNonNull (availability);
        Objects.requireNonNull(today);
        Volunteer volunteer = loadVolunteer(nickname);
        availability.ensureConsistency ();
        MonthlyAvailability sanitized = new MonthlyAvailability (
                availability.getReferenceMonth (),
                availability.getPreferredDays (),
                availability.getWeeklyFrequency (),
                availability.getSubmittedOn ()
        );
        volunteer.registerAvailability (sanitized, today);
        volunteerRepository.save(volunteer);
    }

    @Override
    public Optional<MonthlyAvailability> loadAvailability(String nickname, YearMonth month) {
        Objects.requireNonNull (month);
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.findAvailability (month);
    }

    @Override
    public List<AssignedShift> loadSchedule(String nickname, YearMonth month) {
        Objects.requireNonNull (month);
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.getShiftsForMonth (month);
    }

    @Override
    public void assignShifts(String nickname, YearMonth month, List<AssignedShift> shifts) {
        Objects.requireNonNull (month);
        Objects.requireNonNull (shifts);
        Volunteer volunteer = loadVolunteer(nickname);
        volunteer.assignShifts (month, shifts);
        volunteerRepository.save(volunteer);
    }

    private Volunteer loadVolunteer(String nickname){
        String sanitized = requireNonBlank(nickname, "Il nickname del volontario non può essere vuoto");
        return volunteerRepository.findByNickname (sanitized)
                .orElseThrow (() -> new IllegalArgumentException ("Volontario non trovato: " + sanitized));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public boolean isFirstAccessPending(String nickname) {
        Volunteer volunteer = loadVolunteer(nickname);
        return volunteer.isFirstAccessPending ();
    }

    @Override
    public void verifyDefaultCredentials(String nickname, String password) {
        Volunteer volunteer = loadVolunteer (nickname);
        if (!volunteer.isFirstAccessPending ())
            throw new IllegalStateException ("Le credenziali personali sono già state impostate");
        String sanitizedPassword = requireNonBlank(password, "la Password di default non può essere nulla");
        if (!volunteer.passwordMatches (sanitizedPassword))
            throw new IllegalArgumentException ("Credenziali di primo accesso non valide");

        defaultCredentialsValidated.add(volunteer.getNickname ());
    }

    @Override
    public void setPersonalCredentials(String currentNickname, String newNickname, String password) {
        Volunteer volunteer = loadVolunteer(currentNickname);
        if (!volunteer.isFirstAccessPending ())
            throw new IllegalStateException ("Le credenziali personali sono state impostate");

        String sanitizedPassword = requireNonBlank(password, "La nuova password non può essere nulla");
        String sanitizedNickname = requireNonBlank(newNickname, "Il nuovo nickname non può essere vuoto");

        String current = volunteer.getNickname();
        if (current.equalsIgnoreCase(sanitizedNickname))
            throw new IllegalArgumentException("Il nuovo nickname deve essere diverso da quello assegnato");

        if (volunteer.passwordMatches(sanitizedPassword))
            throw new IllegalArgumentException("La nuova password deve essere diversa da quella assegnata");

        if (!current.equalsIgnoreCase(sanitizedNickname) && volunteerRepository.findByNickname(sanitizedNickname).isPresent())
            throw new IllegalArgumentException("Nickname già presente");

        if(!defaultCredentialsValidated.remove(current))
            throw new IllegalStateException ("Credenziali di default non ancora verificate");

        volunteerRepository.deleteByNickname(current);
        volunteer.setNickname(sanitizedNickname);
        volunteer.setPersonalCredentials (sanitizedPassword);
        volunteerRepository.save(volunteer);
    }

    @Override
    public boolean verifyLogin(String nickname, String password) {
        if (nickname == null || password == null) return false;
        String sanitizedNick = nickname.trim();
        String sanitizedPassword = password.trim();
        if(sanitizedNick.isEmpty() || sanitizedPassword.isEmpty()) return false;

        Optional<Volunteer> volunteer = volunteerRepository.findByNickname (sanitizedNick);
        if (volunteer.isEmpty ()) return false;

        Volunteer loaded = volunteer.get();
        if (loaded.isFirstAccessPending ()) return false;

        return loaded.passwordMatches (sanitizedPassword);
    }
}
