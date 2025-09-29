package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.SystemSettings;
import java.util.Optional;

public interface SettingRepository {

    public Optional<SystemSettings> load();
    public void save(SystemSettings settings);
    public boolean exists();
}
