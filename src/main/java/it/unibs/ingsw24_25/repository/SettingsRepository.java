package it.unibs.ingsw24_25.repository;

import it.unibs.ingsw24_25.model.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<SystemSettings, Long> {
}
