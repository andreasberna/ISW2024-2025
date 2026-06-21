package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "volunteers")
public class Volunteer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean firstAccessPending = true;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "volunteer_visit_type",
        joinColumns = @JoinColumn(name = "volunteer_id"),
        inverseJoinColumns = @JoinColumn(name = "visit_type_id")
    )
    private List<VisitType> visitsAttending = new ArrayList<>();

    @OneToMany(mappedBy = "volunteer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<MonthlyAvailability> availabilities = new ArrayList<>();

    @OneToMany(mappedBy = "volunteer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<AssignedShift> scheduledShifts = new ArrayList<>();

    protected Volunteer() {}

    public Volunteer(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
    }

    public Optional<MonthlyAvailability> findAvailability(YearMonth month) {
        return this.availabilities.stream()
                .filter(a -> a.getReferenceMonth().equals(month))
                .findFirst();
    }
    
    public List<MonthlyAvailability> getAvailabilities() {
        return Collections.unmodifiableList(availabilities);
    }

    public void registerAvailability(MonthlyAvailability availability) {
        this.availabilities.removeIf(a -> a.getReferenceMonth().equals(availability.getReferenceMonth()));
        availability.setVolunteer(this);
        this.availabilities.add(availability);
    }
    
    public void removeAvailability(YearMonth month) {
        this.availabilities.removeIf(a -> a.getReferenceMonth().equals(month));
    }

    public List<AssignedShift> getScheduledShifts() {
        return Collections.unmodifiableList(scheduledShifts);
    }

    public List<AssignedShift> getShiftsForMonth(YearMonth month) {
        return this.scheduledShifts.stream()
                .filter(s -> s.getMonth().equals(month))
                .collect(Collectors.toList());
    }

    public void assignShifts(YearMonth month, List<AssignedShift> shifts) {
        this.scheduledShifts.removeIf(s -> s.getMonth().equals(month));
        shifts.forEach(s -> s.setVolunteer(this));
        this.scheduledShifts.addAll(shifts);
    }
    
    public void removeScheduledShifts(YearMonth month) {
        this.scheduledShifts.removeIf(s -> s.getMonth().equals(month));
    }
    
    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPassword() { return password; }
    public boolean isFirstAccessPending() { return firstAccessPending; }
    public void setPersonalCredentials(String encodedPassword) {
        this.password = Objects.requireNonNull(encodedPassword);
        this.firstAccessPending = false;
    }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
    public List<VisitType> getVisitsAttending() { return visitsAttending; }
    public void setVisitsAttending(List<VisitType> visitsAttending) { this.visitsAttending = visitsAttending; }
    public void addVisit(VisitType visitType) {
        if (!this.visitsAttending.contains(visitType)) {
            this.visitsAttending.add(visitType);
        }
    }

    public boolean passwordMatches(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }
}