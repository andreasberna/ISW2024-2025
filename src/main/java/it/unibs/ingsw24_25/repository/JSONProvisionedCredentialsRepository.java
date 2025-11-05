package it.unibs.ingsw24_25.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

public class JSONProvisionedCredentialsRepository implements ProvisionedCredentialsRepository {

    private final Path file;
    private ProvisionedCredentialStore store;

    public JSONProvisionedCredentialsRepository(Path file) {
        this.file = Objects.requireNonNull(file, "Il file di credenziali non può essere nullo");
        this.store = new ProvisionedCredentialStore();
        init();
    }

    private synchronized void init() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            if (Files.exists(file) && Files.size(file) > 0) {
                String json = Files.readString(file);
                ProvisionedCredentialStore loaded = JSONSupport.deserializeProvisionedCredentials(json);
                if (loaded != null) {
                    this.store = loaded;
                }
            }
        } catch (IOException e) {
            System.err.println("Warn: impossibile leggere " + file + " -> archivio credenziali vuoto");
            this.store = new ProvisionedCredentialStore();
        }
    }

    private synchronized void persist() {
        try {
            String json = JSONSupport.serializeProvisionedCredentials(store);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            Files.move(tmp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Persistenza delle credenziali provisionate fallita", e);
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public synchronized Optional<String> findConfiguratorPassword(String nickname) {
        String sanitized = sanitize(nickname);
        if (sanitized == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.getConfigurators().get(sanitized));
    }

    @Override
    public synchronized Optional<String> findVolunteerPassword(String nickname) {
        String sanitized = sanitize(nickname);
        if (sanitized == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.getVolunteers().get(sanitized));
    }

    @Override
    public synchronized void registerConfiguratorCredential(String nickname, String password) {
        String sanitizedNick = sanitize(nickname);
        String sanitizedPassword = sanitize(password);
        if (sanitizedNick == null || sanitizedPassword == null) {
            throw new IllegalArgumentException("Credenziali di default non valide");
        }
        store.getConfigurators().put(sanitizedNick, sanitizedPassword);
        persist();
    }

    @Override
    public synchronized void registerVolunteerCredential(String nickname, String password) {
        String sanitizedNick = sanitize(nickname);
        String sanitizedPassword = sanitize(password);
        if (sanitizedNick == null || sanitizedPassword == null) {
            throw new IllegalArgumentException("Credenziali di default non valide");
        }
        store.getVolunteers().put(sanitizedNick, sanitizedPassword);
        persist();
    }

    @Override
    public synchronized void consumeConfiguratorCredential(String nickname) {
        String sanitized = sanitize(nickname);
        if (sanitized == null) {
            return;
        }
        if (store.getConfigurators().remove(sanitized) != null) {
            persist();
        }
    }

    @Override
    public synchronized void consumeVolunteerCredential(String nickname) {
        String sanitized = sanitize(nickname);
        if (sanitized == null) {
            return;
        }
        if (store.getVolunteers().remove(sanitized) != null) {
            persist();
        }
    }

    @Override
    public synchronized boolean hasConfiguratorCredential(String nickname) {
        String sanitized = sanitize(nickname);
        if (sanitized == null) {
            return false;
        }
        return store.getConfigurators().containsKey(sanitized);
    }

    @Override
    public synchronized boolean hasVolunteerCredential(String nickname) {
        String sanitized = sanitize(nickname);
        if (sanitized == null) {
            return false;
        }
        return store.getVolunteers().containsKey(sanitized);
    }

    @Override
    public synchronized boolean hasAnyConfiguratorCredential() {
        return !store.getConfigurators().isEmpty();
    }
}
