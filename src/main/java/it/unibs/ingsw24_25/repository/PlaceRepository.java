package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {
    Optional<Place> findByPlaceTitle(String placeTitle);
}
