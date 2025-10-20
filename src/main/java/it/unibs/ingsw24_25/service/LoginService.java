package it.unibs.ingsw24_25.service;

public interface LoginService {
    boolean isFirstAccessPending(String nickname);

    void verifyDefaultCredentials(String nickname, String password);

    void setPersonalCredentials(String nickname, String password);

    boolean verifyLogin(String nickname, String password);
}
