package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Configurator;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JSONConfiguratorRepository implements ConfiguratorRepository {

    private Path file;
    private Map<String, Configurator> cache;

    public JSONConfiguratorRepository(Path file) {
        this.file = file;
        init();
    }

    private void init(){
        try{
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if(Files.exists(file) && Files.size (file) > 0){
                String json = Files.readString (file);
                this.cache = JSONSupport.deserializeConfigurator(json);
            }else this.cache = null;
        } catch(IOException e){
            System.err.println("Warn: impossibile leggere " + file + " -> credenziali assenti");
            this.cache = null;
        }
    }

    private void persist(){
        try{
            String json = JSONSupport.serializeConfigurator(cache);
            Path tmp = file.resolveSibling (file.getFileName() + ".tmp");
            Files.writeString (tmp, json);
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            throw new RuntimeException ("Persistenza credenziali fallita su " + file, e);
        }
    }


    @Override
    public Optional<Map<String, Configurator>> load() {
        return Optional.ofNullable(cache);
    }

    @Override
    public void save(Configurator configurator) {
        Objects.requireNonNull (configurator, "Configuratore nullo");
        cache.put (configurator.getNickname (),  configurator);
        persist();
    }

    @Override
    public boolean exists() {
        return cache != null;
    }
}
