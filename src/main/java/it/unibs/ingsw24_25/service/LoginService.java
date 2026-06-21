package it.unibs.ingsw24_25.service;

import org.springframework.transaction.annotation.Transactional;

public interface LoginService {
    boolean isFirstAccessPending(String nickname);

    @Transactional
    void verifyDefaultCredentials(String nickname, String password);

    default void setPersonalCredentials(String nickname, String password) {
        setPersonalCredentials(nickname, nickname, password);
    }

    @Transactional
    void setPersonalCredentials(String currentNickname, String newNickname, String password);

    boolean verifyLogin(String nickname, String password);
}
