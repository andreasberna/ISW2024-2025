package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Volunteer;
import java.util.List;
import java.util.Optional;

public interface VolunteerRepository {

    Optional<Volunteer> findByNickname(String nickname);
    List<Volunteer> findAll();
    void save(Volunteer volunteer);
    void deleteByNickname(String nickname);
    boolean exist();
};
