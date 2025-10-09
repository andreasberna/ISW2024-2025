package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.VisitState;
import it.unibs.ingsw24_25.model.VisitType;
import java.util.List;
import java.util.Optional;

public interface VisitTypeRepository {

    public Optional<VisitType> findById(String id);
    public List<VisitType> findByPlace(String placeID);
    public List<VisitType> findAll();
    public List<VisitType> findByState(VisitState... states);
    public void save(VisitType visitType);
    public void deleteById(String id);

}
