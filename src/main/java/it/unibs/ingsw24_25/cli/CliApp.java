package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

public class CliApp implements Runnable {
    private static final String DEFAULT_PROMPT = "> ";
    private static final String EXIT_COMMAND = "exit";
    private static final String INVALID_COMMAND_MESSAGE = "Comando non valido";
    private static final String MENU_SEPARATOR = "-----------------------";

    private final PromptReader reader;
    private final Printer printer;
    private final CommandRouter router;

    private volatile boolean running;

    public CliApp(PromptReader reader, Printer printer, CommandRouter router) {
        this.reader = Objects.requireNonNull(reader, "reader non puù essere nullo");
        this.printer = Objects.requireNonNull(printer, "printer non può essere nullo");
        this.router = Objects.requireNonNull(router, "router non può essere nullo");
    }

    @Override
    public void run() {
        running = true;
        printer.println (MENU_SEPARATOR);
        printer.println ("Benvenuto!");
        printer.println (MENU_SEPARATOR);
        while (running) {
            showMenu();
            String command = reader.readLine (DEFAULT_PROMPT);
            if(command.equalsIgnoreCase (EXIT_COMMAND)) {
                running = false;
                continue;
            }
            executeCommand(command);
        }
        printer.println ("Arrivederci!");
    }

    public void start(){
        new Thread(this).start ();
    }
    private void executeCommand(String command) {
        if (command == null || command.isBlank ()) {
            printer.println (INVALID_COMMAND_MESSAGE);
            return;
        }
        if (!router.route(command.trim()))
            printer.println (INVALID_COMMAND_MESSAGE);
    }

    private void showMenu(){
        printer.println(MENU_SEPARATOR);
        printer.println("Scegli un'opzione");
        printer.println("setup     - Inserisci luoghi, visite e volontari");
        printer.println("list      - Visualizza le informazioni registrate");
        printer.println("settings  - Configura i parametri di servizio");
        printer.println("exit - Esci");
        printer.println(MENU_SEPARATOR);
    }

    public void stop(){
        running = false;
    }
    public boolean isRunning() {
        return running;
    }


}
