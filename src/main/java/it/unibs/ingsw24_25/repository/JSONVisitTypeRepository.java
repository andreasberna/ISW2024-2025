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

    private Path file;
    private Map<String, VisitType> cache;

    public JSONVisitTypeRepository(Path file) {
        this.file = file;
        this.cache = new HashMap<> ();
        init();
    }

    private void init(){
        try{
            Files.createDirectories (file.getParent ());
            if(Files.exists (file) && Files.size (file) > 0){
                String json = Files.readString (file);
                Map<String, VisitType> loaded = JSONSupport.deserializeVisitTypeMap (json);
                if(loaded != null) cache.putAll (loaded);
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
        List<VisitType> ret = cache.values ().stream ().filter (visitType -> visitType.getPlace ().getPlaceTitle ().equals (placeID)).collect (Collectors.toList ());
        return ret;
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
        cache.put (visitType.getVisitTitle (),  visitType);
        persist();
    }

    @Override
    public void deleteById(String id) {
        if(cache.remove (id) != null) persist ();
    }
}
