package it.unibs.ingsw24_25.cli;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public class CommandRouter {
    private final Map<String, BooleanSupplier> handlers = new HashMap<> ();

    public CommandRouter() {
    }
    public CommandRouter(ConfiguratorCommandHandler handler) {
        this();
        Objects.requireNonNull (handler, "ConfiguratorCommandHandler non può essere nullo");
        register("1", handler::openSetupMenu);
        register("setup", handler::openSetupMenu);
        register("2", handler::openListMenu);
        register("list", handler::openListMenu);
        register("3", handler::openSettingsMenu);
        register("settings", handler::openSettingsMenu);

    }

    public CommandRouter register(String command, BooleanSupplier handler) {
        Objects.requireNonNull (command, "Command non può essere nullo");
        Objects.requireNonNull (handler, "CommandHandler non può essere nullo");

        String normalized = normalize(command);
        if(normalized.isEmpty())
            throw new IllegalArgumentException("Command non può essere vuoto");
        if(handlers.putIfAbsent(normalized, handler) != null)
            throw new IllegalArgumentException("Comando già registrato: " + normalized);
        return this;
    }

    public boolean route (String command) {
        if (command == null) return false;

        String normalized = normalize(command);
        if(normalized.isEmpty()) return false;

        BooleanSupplier handler = handlers.get(normalized);
        if (handler == null) return false;

        return handler.getAsBoolean();
    }

    public String normalize(String command) {
        return command.trim ().toLowerCase (Locale.ITALY);
    }

}
