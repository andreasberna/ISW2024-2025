package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Place;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository {

    public Optional<Place> findById(String id);
    public List<Place> findAll();
    public void save(Place place);
    public void deleteById(String id);

}
