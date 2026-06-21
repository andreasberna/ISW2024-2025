package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.service.dtos.ConfiguratorSetupData;
import it.unibs.ingsw24_25.service.dtos.VolunteerSetupData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class FirstAccessService {

    private final ConfiguratorService configuratorService;
    private final VolunteerService volunteerService;

    public FirstAccessService(ConfiguratorService configuratorService, VolunteerService volunteerService) {
        this.configuratorService = Objects.requireNonNull(configuratorService, "ConfiguratorService non può essere nullo");
        this.volunteerService = Objects.requireNonNull(volunteerService, "VolunteerService non può essere nullo");
    }

    /**
     * Esegue il setup completo del primo accesso per un configuratore in un'unica operazione transazionale.
     *
     * @param data I dati necessari per il setup, inclusi vecchie e nuove credenziali e impostazioni.
     */
    @Transactional
    public void setupConfigurator(ConfiguratorSetupData data) {
        Objects.requireNonNull(data, "I dati di setup non possono essere nulli");

        // 1. Verifica le credenziali di default
        configuratorService.verifyDefaultCredentials(data.defaultNickname(), data.defaultPassword());

        // 2. Imposta le nuove credenziali personali
        configuratorService.setPersonalCredentials(data.defaultNickname(), data.newNickname(), data.newPassword());

        // 3. Imposta l'ambito territoriale
        configuratorService.setTerritorialScope(data.territorialScope());

        // 4. Imposta il numero massimo di partecipanti
        configuratorService.setMaxPeoplePerSubscription(data.maxParticipants());
    }

    /**
     * Esegue il setup completo del primo accesso per un volontario in un'unica operazione transazionale.
     *
     * @param data I dati necessari per il setup, incluse vecchie e nuove credenziali.
     * @return Il nuovo nickname del volontario.
     */
    @Transactional
    public String setupVolunteer(VolunteerSetupData data) {
        Objects.requireNonNull(data, "I dati di setup non possono essere nulli");

        // 1. Verifica le credenziali di default
        volunteerService.verifyDefaultCredentials(data.defaultNickname(), data.defaultPassword());

        // 2. Imposta le nuove credenziali personali
        volunteerService.setPersonalCredentials(data.defaultNickname(), data.newNickname(), data.newPassword());

        return data.newNickname();
    }

    /**
     * Controlla se ci sono configuratori in attesa del primo accesso.
     * @return true se ci sono configuratori da inizializzare, false altrimenti.
     */
    public boolean hasPendingConfigurators() {
        return configuratorService.hasPendingConfiguratorSeeds();
    }
}