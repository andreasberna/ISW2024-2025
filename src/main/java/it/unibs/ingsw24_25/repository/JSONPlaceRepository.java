package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Place;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class JSONPlaceRepository implements PlaceRepository {
    private final Path file;
    private final Map<String, Place> cache;
    private final MonthlyVisitPlanRepository planRepository;

    public JSONPlaceRepository(Path file) {
        this(file, null);
    }

    public JSONPlaceRepository(Path file, MonthlyVisitPlanRepository planRepository) {
        this.file = file;
        this.planRepository = planRepository;
        this.cache = new HashMap<> ();
        init();
    }

    private void init(){
        try{
            Files.createDirectories (file.getParent ());
            if(Files.exists (file) && Files.size (file) > 0){
                String json = Files.readString (file);
                Map<String, Place> loaded = JSONSupport.deserializePlaceMap (json);
                if(loaded != null) cache.putAll(loaded);
            }
        }catch(IOException e){
            System.err.println("Warn: impossibile leggere " + file + " -> cache vuota");
        }
    }

    private void persist(){
        try {
            String json = JSONSupport.serializePlaceMap (cache);
            Path tmp = file.resolveSibling (file.getFileName () + ".tmp");
            Files.writeString (tmp, json);
            Files.move (tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            throw new RuntimeException ("Persistenza fallita su " + file, e);
        }
    }

    @Override
    public Optional<Place> findById(String id) {
        return Optional.ofNullable (cache.get (id));
    }

    @Override
    public List<Place> findAll() {
        return new ArrayList<> (cache.values());
    }

    @Override
    public void save(Place place) {
        Objects.requireNonNull (place, "place nullo");
        cache.put (place.getPlaceTitle (),  place);
        persist ();
    }

    @Override
    public void deleteById(String id) {
        Place removed = cache.remove(id);
        if(removed != null) {
            persist();
            if (planRepository != null) {
                List<String> visitTypeIds = removed.getVisits().stream()
                        .map(VisitType::getId)
                        .filter(Objects::nonNull)
                        .toList();
                planRepository.removePlannedVisitsByVisitTypes(visitTypeIds);
            }
        }
    }
}
