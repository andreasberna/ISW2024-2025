package it.unibs.ingsw24_25.model;

import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    /**
     * Costruttore richiesto da JPA.
     */
    protected Beneficiary() {}

    public Beneficiary(String username, String password, String fullName) {
        this.username = Objects.requireNonNull(username, "Username non può essere nullo");
        this.password = Objects.requireNonNull(password, "Password non può essere nulla");
        this.fullName = Objects.requireNonNull(fullName, "Full name non può essere nullo");
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = Objects.requireNonNull(password, "Password non può essere nulla");
    }

    public String getFullName() {
        return fullName;
    }

    public boolean passwordMatches(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Beneficiary that = (Beneficiary) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}