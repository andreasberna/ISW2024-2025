package it.unibs.ingsw24_25.service;

public interface LoginService {
    boolean isFirstAccessPending(String nickname);

    void verifyDefaultCredentials(String nickname, String password);

    default void setPersonalCredentials(String nickname, String password) {
        setPersonalCredentials(nickname, nickname, password);
    }

    void setPersonalCredentials(String currentNickname, String newNickname, String password);

    boolean verifyLogin(String nickname, String password);
}
