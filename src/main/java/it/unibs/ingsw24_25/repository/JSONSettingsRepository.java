package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.SystemSettings;
import it.unibs.ingsw24_25.util.JSONSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class JSONSettingsRepository implements SettingsRepository {
    private Path file;
    private SystemSettings cache;

    public JSONSettingsRepository(Path file) {
        this.file = file;
        init();
    }

    private void init(){
        try{
            Files.createDirectories(file.getParent());
            if(Files.exists(file) && Files.size(file) > 0){
                String json = Files.readString(file);
                this.cache = JSONSupport.deserializeSystemSettings (json);
            }else this.cache = null;
        }catch(IOException e){
            System.err.println("Warn: lettura " + file + "fallita -> settings non inizializzati");
            this.cache = null;
        }
    }

    private void persist(){
        try{
            String json = JSONSupport.serializeSystemSettings(cache);
            Path tmp = file.resolveSibling (file.getFileName() + ".tmp");
            Files.writeString (tmp, json);
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            throw new RuntimeException ("Persistenza settings fallita su " + file, e);
        }
    }

    @Override
    public Optional<SystemSettings> load() {
        return Optional.ofNullable(cache);
    }

    @Override
    public void save(SystemSettings settings) {
        Objects.requireNonNull (settings);
        if(cache != null && !Objects.equals(cache.getTerritorialScope (), settings.getTerritorialScope())) throw new IllegalStateException ("Ambito Territoriale è già stato definito e non è modificabile");
        this.cache = settings;
        persist ();
    }

    @Override
    public boolean exists() {
        return cache != null;
    }

    public void updateMaxPeoplePerSubscription(int newValue){
        if(cache == null) throw new IllegalStateException ("Settings non ancora inizializzati");
        this.cache = new SystemSettings (
                cache.getTerritorialScope (),
                newValue,
                new ArrayList<> (cache.getExcludedDates ())
        );
    }
}
