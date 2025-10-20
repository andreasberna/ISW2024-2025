package it.unibs.ingsw24_25.model;

import it.unibs.ingsw24_25.util.AvailabilitySubmissionPolicy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class Volunteer {
    private String nickname;
    private String password;
    private boolean personalCredentialsDefined;
    private List<VisitType> visitsAttending = new ArrayList<> ();
    private Map<YearMonth, MonthlyAvailability> availabilities = new HashMap<> ();
    private Map<YearMonth, List<AssignedShift>> scheduledShifts = new HashMap<> ();

    public Volunteer(String nickname, String password) {
        this.nickname = Objects.requireNonNull(nickname, "nickname nullo");
        setDefaultPassword(password);
    }
    Volunteer() {}

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        setDefaultPassword(password);
    }
    public List<VisitType> getVisitsAttending() {
        return ensureVisitInitialized();
    }

    public void setVisitsAttending(List<VisitType> visitsAttending) {
        if (visitsAttending == null) this.visitsAttending = new ArrayList<> ();
        else  this.visitsAttending = new ArrayList<> (visitsAttending);
    }

    public boolean isFirstAccessPending() {
        return !personalCredentialsDefined;
    }
    public void setPersonalCredentials(String password){
        this.password = requireNonBlank (password, "password nullo");
        this.personalCredentialsDefined = true;
    }
    public boolean passwordMatches(String candidate){
        if (candidate == null) return false;

        return this.password.equals (candidate.trim ());
    }

    public void addVisit(VisitType visit){
        if(visit != null) ensureVisitInitialized ().add (visit);
    }
    public void removeVisit(VisitType visit){
        if (visit != null) ensureVisitInitialized ().remove (visit);
    }

    private List<VisitType> ensureVisitInitialized() {
        if(this.visitsAttending == null) this.visitsAttending = new ArrayList<> ();
        return this.visitsAttending;
    }

    public Map<YearMonth, MonthlyAvailability> getAvailabilities() {
        ensureAvailabilityInitialized();
        Map<YearMonth, MonthlyAvailability> copy = new HashMap<> ();
        availabilities.forEach((month, availability) -> {
            availability.ensureConsistency ();
            copy.put(month, new MonthlyAvailability (
                    availability.getReferenceMonth (),
                    availability.getPreferredDays (),
                    availability.getWeeklyFrequency (),
                    availability.getSubmittedOn ()
            ));
        });
        return Collections.unmodifiableMap(copy);
    }
    public Optional<MonthlyAvailability> findAvailability(YearMonth month) {
        Objects.requireNonNull(month, "il mese non può essere nullo");
        ensureAvailabilityInitialized();
        MonthlyAvailability availability = availabilities.get (month);
        if (availability == null) {
            return Optional.empty();
        }
        availability.ensureConsistency ();
        MonthlyAvailability copy = new MonthlyAvailability (
                availability.getReferenceMonth (),
                availability.getPreferredDays (),
                availability.getWeeklyFrequency (),
                availability.getSubmittedOn ()
        );
        return Optional.of(copy);
    }
    public void registerAvailability(MonthlyAvailability availability, LocalDate today) {
        Objects.requireNonNull(availability, "la disponibilità non può essere nulla");
        Objects.requireNonNull (today);
        availability.ensureConsistency ();
        MonthlyAvailability sanitized = new MonthlyAvailability (
                availability.getReferenceMonth (),
                availability.getPreferredDays (),
                availability.getWeeklyFrequency (),
                availability.getSubmittedOn ()
        );
        if (!AvailabilitySubmissionPolicy.isWindowOpen (sanitized.getReferenceMonth (), today))
            throw new IllegalStateException ("La finestra di caricamento è chiusa per il mese " + sanitized.getReferenceMonth());

        ensureAvailabilityInitialized ();
        availabilities.put (sanitized.getReferenceMonth (), sanitized);
    }
    public Map<YearMonth, List<AssignedShift>> getScheduledShifts() {
        ensureShiftsInitialized ();
        Map<YearMonth, List<AssignedShift>> copy = new HashMap<> ();
        scheduledShifts.forEach((month, shifts) -> copy.put(month, List.copyOf (shifts)));

        return Collections.unmodifiableMap(copy);
    }
    public List<AssignedShift> getShiftsForMonth(YearMonth month) {
        Objects.requireNonNull(month, "il mese non può essere nulla");
        ensureAvailabilityInitialized();
        List<AssignedShift> shifts = scheduledShifts.get (month);
        if (shifts == null) {return List.of();}

        return List.copyOf (shifts);

    }
    public void assignShifts(YearMonth month, List<AssignedShift> shifts) {
        Objects.requireNonNull(month, "il mese non può essere nullo");
        Objects.requireNonNull(shifts, "i turni non possono essere nulli");
        ensureShiftsInitialized ();
        List<AssignedShift> copies = shifts.stream()
                .filter (Objects::nonNull)
                .map(shift -> {
                    TimeSlot slot = Objects.requireNonNull (shift.getSlot ());
                    return new AssignedShift (
                            shift.getDate (),
                            shift.getVisitTypeId (),
                            new TimeSlot (
                                    slot.getDay (),
                                    slot.getStartTime (),
                                    slot.getDuration ()
                            )
                    );
                })
                .toList ();
        scheduledShifts.put(month, new ArrayList<> (copies));
    }

    private void ensureAvailabilityInitialized() {
        if (availabilities == null) {
            availabilities = new HashMap<>();
        }
    }

    private void ensureShiftsInitialized() {
        if (scheduledShifts == null) {
            scheduledShifts = new HashMap<>();
        }
    }
    public boolean isPersonalCredentialsDefined() {
        return personalCredentialsDefined;
    }

    public void setPersonalCredentialsDefined(boolean personalCredentialsDefined) {
        this.personalCredentialsDefined = personalCredentialsDefined;
    }

    private void setDefaultPassword(String password) {
        this.password = requireNonBlank(password, "Password nulla");
        this.personalCredentialsDefined = false;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
