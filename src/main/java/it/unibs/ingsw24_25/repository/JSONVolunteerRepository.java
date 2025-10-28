package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class JSONVolunteerRepository implements VolunteerRepository{

    private final Path file;
    private final Map<String,Volunteer> cache;
    private final MonthlyVisitPlanRepository planRepository;


    public JSONVolunteerRepository(Path file) {
        this(file, null);
    }
    public JSONVolunteerRepository(Path file,  MonthlyVisitPlanRepository planRepository) {
        this.file = file;
        this.planRepository = planRepository;
        this.cache = new HashMap<> ();
        init();
    }
    private void init() {
        try{
            Files.createDirectories(file.getParent());
            if(Files.exists(file) && Files.size(file) > 0) {
                String json = Files.readString (file);
                Map<String, Volunteer> loaded = JSONSupport.deserializeVolunteerMap(json);
                if(loaded != null) cache.putAll(loaded);
            }
        }catch (IOException e){
            System.err.println("Warn: impossibile leggere" + file + " -> cache vuota");
        }
    }

    private void persist(){
        try {
            String json = JSONSupport.serializeVolunteerMap (cache);
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
    public Optional<Volunteer> findByNickname(String nickname) {
        return Optional.ofNullable(cache.get(nickname));
    }

    @Override
    public List<Volunteer> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public void save(Volunteer v) {
        Objects.requireNonNull(v, "Volunteer nullo");
        cache.put(v.getNickname(), v);
        persist();
    }

    @Override
    public void deleteByNickname(String nickname) {
        Volunteer removed = cache.remove(nickname);
        if(removed != null) {
            persist();
            if (planRepository != null) {
                planRepository.removeVolunteerAssignments(nickname);
            }
        }
    }

    @Override
    public boolean exist() {
        return !cache.isEmpty();
    }
}
