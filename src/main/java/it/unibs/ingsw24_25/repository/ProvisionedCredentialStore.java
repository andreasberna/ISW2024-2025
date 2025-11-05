package it.unibs.ingsw24_25.repository;

import java.util.HashMap;
import java.util.Map;

public class ProvisionedCredentialStore {
    private Map<String, String> configurators;
    private Map<String, String> volunteers;

    public ProvisionedCredentialStore() {
        this.configurators = new HashMap<> ();
        this.volunteers = new HashMap<>();
    }

    Map<String, String> getConfigurators() {
        if (configurators == null) {
            configurators = new HashMap<>();
        }
        return configurators;
    }

    Map<String, String> getVolunteers() {
        if (volunteers == null) {
            volunteers = new HashMap<>();
        }
        return volunteers;
    }
}
