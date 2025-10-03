package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Configurator;

import java.util.Map;
import java.util.Optional;

public interface ConfiguratorRepository {
    Optional<Map<String, Configurator>> load();

    void save(Configurator configurator);

    boolean exists();
}
