package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Beneficiary;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class JSONBeneficiaryRepository implements BeneficiaryRepository {

    private final Path file;
    private final Map<String, Beneficiary> cache = new HashMap<> ();

    public JSONBeneficiaryRepository(Path file) {
        this.file = file;
        init();
    }

    private void init() {
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file) && Files.size(file) > 0) {
                String json = Files.readString(file);
                Map<String, Beneficiary> loaded = JSONSupport.deserializeBeneficiaryMap(json);
                if (loaded != null) {
                    cache.putAll(loaded);
                }
            }
        } catch (IOException e) {
            System.err.println("Warn: impossibile leggere " + file + " -> cache fruitori vuota");
        }
    }
    private void persist(){
        try {
            String json = JSONSupport.serializeBeneficiaryMap(cache);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Persistenza fruitori fallita su " + file, e);
        }
    }

    @Override
    public Optional<Beneficiary> findByUsername(String username) {
        if (username == null) {return Optional.empty();}

        return Optional.ofNullable(cache.get(username));
    }

    @Override
    public List<Beneficiary> findAll() {
        return new ArrayList<> (cache.values());
    }

    @Override
    public void save(Beneficiary beneficiary) {
        Objects.requireNonNull(beneficiary, "Il fruitore non può essere nullo");
        cache.put(beneficiary.getUsername(), beneficiary);
        persist();
    }

    @Override
    public void deleteByUsername(String username) {
        if (username == null) {return;}

        if (cache.remove (username) != null) persist();
    }
}
