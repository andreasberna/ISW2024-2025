package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    Optional<Volunteer> findByNickname(String nickname);

    @Transactional
    void deleteByNickname(String nickname);
}
