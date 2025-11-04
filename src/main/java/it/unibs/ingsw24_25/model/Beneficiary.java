package it.unibs.ingsw24_25.model;

public class Beneficiary {

    private String username;
    private String password;
    private String fullName;

    public Beneficiary(String username, String password, String fullName) {
        this. username = requireNonBlank(username, "Lo username del fruitore non può essere vuoto");
        this. username = requireNonBlank(username, "Lo username del fruitore non può essere vuoto");
        this.fullName = requireNonBlank(fullName, "Il nome del fruitore non può essere vuoto");
    }

    public Beneficiary(){
        //costruttore per la (de)serializzazione
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this. username = requireNonBlank(username, "Lo username del fruitore non può essere vuoto");
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this. username = requireNonBlank(username, "Lo username del fruitore non può essere vuoto");
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = requireNonBlank(fullName, "Il nome del fruitore non può essere nullo");
    }

    public boolean passwordMatches(String candidate){
        if (candidate == null) return false;
        return password.equals (candidate.trim ());
    }

    private String requireNonBlank(String value, String message){
        if (value == null || value.isBlank ())
            throw new IllegalArgumentException(message);
        return value.trim ();
    }

}
