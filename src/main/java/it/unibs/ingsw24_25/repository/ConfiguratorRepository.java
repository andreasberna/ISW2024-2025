package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.Configurator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ConfiguratorRepository extends JpaRepository<Configurator, Long> {

    Optional<Configurator> findByNickname(String nickname);
    
    default Optional<Map<String, Configurator>> load() {
        Map<String, Configurator> map = findAll().stream()
                .collect(Collectors.toMap(Configurator::getNickname, c -> c));
        return map.isEmpty() ? Optional.empty() : Optional.of(map);
    }
    
    default boolean exists() {
        return count() > 0;
    }
}