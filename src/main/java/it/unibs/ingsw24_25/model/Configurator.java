package it.unibs.ingsw24_25.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Configurator {
    @Id
    private String nickname;
    private String password;

    protected Configurator() {}

    public Configurator(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
    }

    public String getNickname() {
        return this.nickname;
    }
    public String getPassword() {
        return this.password;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}