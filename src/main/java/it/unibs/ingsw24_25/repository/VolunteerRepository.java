package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Volunteer;
import java.util.List;
import java.util.Optional;

public interface VolunteerRepository {

    public Optional<Volunteer> findByNickname(String nickname);
    public List<Volunteer> findAll();
    public void save(Volunteer volunteer);
    public void deleteByNickname(String nickname);
};
