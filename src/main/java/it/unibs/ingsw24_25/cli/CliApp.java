package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

public class CliApp implements Runnable {
    private static final String DEFAULT_PROMPT = "> ";
    private static final String DEFUALT_EXIT_COMMAND = "exit";

    private final PromptReader reader;
    private final Printer printer;
    private final CommandRouter router;
    private final String prompt;
    private final String exitCommand;

    private volatile boolean running;

    public CliApp(PromptReader reader, Printer printer, CommandRouter router) {
        this.reader = reader;
        this.printer = printer;
        this.router = router;
        this.prompt = DEFAULT_PROMPT;
        this.exitCommand = DEFUALT_EXIT_COMMAND;
    }

    public CliApp(
            PromptReader reader,
            Printer printer,
            CommandRouter router,
            String prompt,
            String exitCommand) {

        this.reader = Objects.requireNonNull(reader, "reader non può essere nullo");
        this.printer = Objects.requireNonNull(printer, "printer non può essere nullo");
        this.router = Objects.requireNonNull(router, "router non può essere nullo");
        this.prompt = prompt == null ? DEFAULT_PROMPT : prompt;
        this.exitCommand = exitCommand == null ? DEFUALT_EXIT_COMMAND : exitCommand;
    }

    public void start(){
        ensureStartable();
        printer.println ("Digita '" + exitCommand + "' per uscire");
        loop();
    }

    private void ensureStartable() {
        synchronized (this){
            if (running){
                throw new IllegalStateException("CLI già in esecuzione");
            }
            running = true;
        }
    }

    private void loop(){
        while (isRunning()){
            String rawInput = reader.readLine (prompt);
            if(rawInput == null){
                printer.println ("nessun altro input: terminazione");
                stop();
                break;
            }

            String command = rawInput.trim();
            if(command.isEmpty()){continue;}

            if(command.equals(exitCommand)){stop(); break;}

            boolean handled = router.route(command);
            if(!handled){printer.printError ("Comando sconosciuto: " + command);}
        }
    }

    public void stop(){
        boolean wasRunning;
        synchronized (this){
            wasRunning = running;
            running = false;
        }
        if (wasRunning){printer.println("Arrivederci!");}
    }

    public boolean isRunning(){
        return running;
    }

    @Override
    public void run() {
        start();
    }
}
