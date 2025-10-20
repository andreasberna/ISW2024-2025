package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.ConfiguratorServiceImp;

import java.util.Objects;

public class FirstAccessSetup {
    private static final String MENU_SEPARATOR = "-----------------------";
    private static final String INTRO_MESSAGE = "Configurazione iniziale richiesta.";
    private static final String DEFAULT_NICK_PROMPT = "Inserisci il nickname di default: ";
    private static final String DEFAULT_PASS_PROMPT = "Inserisci la password di default: ";
    private static final String DEFAULT_CREDENTIALS_SUCCESS = "Credenziali di default verificate";
    private static final String PERSONAL_NICK_PROMPT = "Imposta un nuovo nickname amministratore: ";
    private static final String PERSONAL_PASS_PROMPT = "Imposta una nuova password amministratore: ";
    private static final String TERRITORIAL_SCOPE_PROMPT = "Definisci l'ambito territoriale del servizio: ";
    private static final String MAX_PEOPLE_PROMPT = "Imposta il numero massimo di partecipanti per iscrizione: ";
    private static final String ERROR_PREFIX = "Errore: ";
    private static final String PERSONAL_CREDENTIALS_SUCCESS = "Credenziali amministratore impostate.";
    private static final String COMPLETION_MESSAGE = "Configurazione iniziale completata.";

    private final ConfiguratorService service;
    private final PromptReader reader;
    private final Printer printer;

    public FirstAccessSetup(ConfiguratorService service, PromptReader reader, Printer printer) {
        this.service = Objects.requireNonNull(service, "Service non può essere nullo");
        this.reader = Objects.requireNonNull(reader, "Reader non può essere nulla");
        this.printer = Objects.requireNonNull(printer, "Printer non può essere nulla");
    }

    public void run(){
        if(!service.isFirstAccessPending (ConfiguratorServiceImp.DEFAULT_NICKNAME)) return;

        printer.println(MENU_SEPARATOR);
        printer.println(INTRO_MESSAGE);
        printer.println(MENU_SEPARATOR);

        verifyDefaultCredentials();
        setPersonalCredentials();
        configureTerritorialScope();
        configureMaxParticipants();

        printer.println (MENU_SEPARATOR);
        printer.println (COMPLETION_MESSAGE);
        printer.println (MENU_SEPARATOR);
    }

    private void verifyDefaultCredentials(){
        boolean verified = false;
        while(!verified){
            String nickname = reader.readLine (DEFAULT_NICK_PROMPT);
            String password = reader.readLine (DEFAULT_PASS_PROMPT);
            try{
                service.verifyDefaultCredentials(nickname, password);
                printer.println (DEFAULT_CREDENTIALS_SUCCESS);
                verified = true;
            } catch (IllegalArgumentException | IllegalStateException e){
                printer.println(ERROR_PREFIX + safeMessage(e));
            }
        }
    }

    private void setPersonalCredentials(){
        boolean stored = false;
        while(!stored) {
            String nickname = reader.readLine (PERSONAL_NICK_PROMPT);
            String password = reader.readLine (PERSONAL_PASS_PROMPT);
            try{
                service.setPersonalCredentials(nickname, password);
                printer.println(PERSONAL_CREDENTIALS_SUCCESS);
                stored = true;
            } catch(IllegalArgumentException | IllegalStateException e){
                printer.println( ERROR_PREFIX + safeMessage(e));
            }
        }
    }

    private void configureTerritorialScope(){
        boolean configured = false;
        while(!configured) {
            String scope = reader.readLine(TERRITORIAL_SCOPE_PROMPT);
            try {
                service.setTerritorialScope(scope);
                configured = true;
            } catch (IllegalArgumentException | IllegalStateException e){
                printer.println (ERROR_PREFIX + safeMessage(e));
            }
        }
    }

    private void configureMaxParticipants(){
        boolean configured = false;
        while(!configured) {
            String rawValue = reader.readLine(MAX_PEOPLE_PROMPT);
            try{
                int value = Integer.parseInt (rawValue);
                service.setMaxPeoplePerSubscription (value);
                configured = true;
            } catch(NumberFormatException e){
                printer.println (ERROR_PREFIX + "Inserire un numero intero valido");
            } catch(IllegalArgumentException | IllegalStateException e){
                printer.println (ERROR_PREFIX + safeMessage(e));
            }
        }
    }

    private String safeMessage(RuntimeException ex){
        String message = ex.getMessage();
        return message == null ? "" : message;
    }
}
