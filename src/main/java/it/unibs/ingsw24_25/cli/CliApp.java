package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public class CliApp implements Runnable {
    private static final String DEFAULT_PROMPT = "> ";
    private static final String EXIT_COMMAND = "exit";
    private static final String INVALID_COMMAND_MESSAGE = "Comando non valido";
    private static final String MENU_SEPARATOR = "-----------------------";

    private final PromptReader reader;
    private final Printer printer;
    private final ConfiguratorCommandHandler configuratorHandler;
    private final VolunteerCommandHandler volunteerHandler;
    private final BeneficiaryCommandHandler beneficiaryHandler;

    private volatile boolean running;

    public CliApp(PromptReader reader, Printer printer,
                  ConfiguratorCommandHandler configuratorHandler,
                  VolunteerCommandHandler volunteerHandler,
                  BeneficiaryCommandHandler beneficiaryHandler) {
        this.reader = Objects.requireNonNull(reader, "reader non puù essere nullo");
        this.printer = Objects.requireNonNull(printer, "printer non può essere nullo");
        this.configuratorHandler = Objects.requireNonNull (configuratorHandler, "configuratorHandler non può essere nullo");
        this.volunteerHandler = Objects.requireNonNull (volunteerHandler, "volunteerHandler non può essere nullo");
        this.beneficiaryHandler = Objects.requireNonNull (beneficiaryHandler, "BeneficiaryHandler non può essere nullo");
    }

    @Override
    public void run() {
        running = true;
        printer.println (MENU_SEPARATOR);
        printer.println ("Benvenuto!");
        printer.println (MENU_SEPARATOR);
        while (running) {
            showEntryMenu();
            String command = reader.readLine (DEFAULT_PROMPT);
            if (command == null){
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }
            String normalized = command.trim ().toLowerCase (Locale.ITALIAN);
            if (normalized.isEmpty ()) {
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }
            if (EXIT_COMMAND.equals (normalized) || "esci".equals (normalized)) {
                running = false;
                continue;
            }

            switch (normalized) {
                case "1", "config", "configuratore", "configurator" -> configuratorHandler.startSession ();
                case "2", "volontario", "volunteer" -> volunteerHandler.startSession ();
                case "3", "fruitore", "beneficiario" ->  beneficiaryHandler.startSession ();
                default -> printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
        printer.println ("Arrivederci!");
    }

    public void start(){
        new Thread(this).start ();
    }

    private void showEntryMenu(){
        printer.println(MENU_SEPARATOR);
        printer.println("Seleziona il profilo di accesso");
        printer.println("1 - Configuratore");
        printer.println("2 - Volontario");
        printer.println("3 - Fruitore");
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
