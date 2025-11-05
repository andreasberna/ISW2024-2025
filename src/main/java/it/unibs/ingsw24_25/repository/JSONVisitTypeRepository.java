package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.VisitState;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class JSONVisitTypeRepository implements VisitTypeRepository {

    private final Path file;
    private final Map<String, VisitType> cache;
    private final MonthlyVisitPlanRepository planRepository;

    public JSONVisitTypeRepository(Path file) {
        this(file, null);
    }

    public JSONVisitTypeRepository(Path file, MonthlyVisitPlanRepository planRepository) {
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
                Map<String, VisitType> loaded = JSONSupport.deserializeVisitTypeMap (json);
                if(loaded != null) {
                    loaded.values().forEach(visitType -> {
                        if (visitType != null) {
                            String identifier = visitType.getId();
                            cache.put(identifier, visitType);
                        }
                    });
                }
            }
        }catch(IOException e){
            System.err.println("Warn: impossibile leggere " + file);
        }
    }

    private void persist(){
        try{
            String json = JSONSupport.serializeVisitTypeMap (cache);
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
    public Optional<VisitType> findById(String id) {
        return Optional.ofNullable (cache.get (id));
    }

    @Override
    public List<VisitType> findByPlace(String placeID) {
        return cache.values ().stream()
                .filter(Objects::nonNull)
                .filter(visitType -> visitType.getPlace() != null && Objects.equals(visitType.getPlace().getPlaceTitle(), placeID))
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitType> findByState(VisitState... states){
        if(states == null || states.length == 0) return List.of();
        Set<VisitState> wanted = EnumSet.copyOf (Arrays.asList(states));
        return cache.values ().stream ()
                .filter (vt -> vt.getState () != null && wanted.contains (vt.getState ()))
                .collect(Collectors.toList());
    }

    @Override
    public List<VisitType> findAll() {
        return new  ArrayList<> (cache.values ());
    }

    @Override
    public void save(VisitType visitType) {
        Objects.requireNonNull (visitType);
        cache.put (visitType.getId (),  visitType);
        persist();
    }

    @Override
    public void deleteById(String id) {
        VisitType removed = cache.remove(id);
        if (removed != null) {
            persist();
            if (planRepository != null) {
                planRepository.removePlannedVisitsByVisitType(removed.getId());
            }
        }
    }
}
