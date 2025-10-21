package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.ConfiguratorServiceImp;
import it.unibs.ingsw24_25.service.VolunteerService;

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
    private static final String ERROR_PREFIX = "[ERRORE]: ";
    private static final String PERSONAL_CREDENTIALS_SUCCESS = "Credenziali amministratore impostate.";
    private static final String COMPLETION_MESSAGE = "Configurazione iniziale completata.";

    private static final String VOLUNTEER_FIRST_ACCESS_HEADER = "Primo accesso volontario";
    private static final String VOLUNTEER_DEFAULT_PASSWORD_PROMPT = "Inserisci la password temporanea (\"back\" per annullare):";
    private static final String VOLUNTEER_NEW_NICKNAME_PROMPT = "Imposta un nuovo nickname personale: ";
    private static final String VOLUNTEER_INVALID_NICKNAME = "Il nickname non può essere vuoto.";
    private static final String VOLUNTEER_NICKNAME_UNCHANGED = "Il nuovo nickname deve essere diverso da quello assegnato dal sistema.";
    private static final String VOLUNTEER_NEW_PASSWORD_PROMPT = "Imposta una nuova password: ";
    private static final String VOLUNTEER_CONFIRM_PASSWORD_PROMPT = "Conferma la nuova password: ";
    private static final String VOLUNTEER_PASSWORD_MISMATCH = "Le password non coincidono.";
    private static final String VOLUNTEER_FIRST_ACCESS_SUCCESS = "Credenziali personali impostate con successo. Nuovo nickname: %s.";
    private static final String OPERATION_ABORTED = "Operazione annullata.";

    private final ConfiguratorService service;
    private final VolunteerService volunteerService;
    private final PromptReader reader;
    private final Printer printer;

    public FirstAccessSetup(ConfiguratorService service,
                            VolunteerService volunteerService,
                            PromptReader reader, Printer printer) {
        this.service = Objects.requireNonNull(service, "Service non può essere nullo");
        this.volunteerService = Objects.requireNonNull (volunteerService);
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

    public String handleVolunteerFirstAccess(String nickname){
        Objects.requireNonNull (nickname, "Il nickname non può essere nullo");

        printer.println (MENU_SEPARATOR);
        printer.println (VOLUNTEER_FIRST_ACCESS_HEADER);
        printer.println (MENU_SEPARATOR);

        while (true) {
            String defaultPassword = reader.readLine (VOLUNTEER_DEFAULT_PASSWORD_PROMPT);
            if (defaultPassword == null){
                printer.println(OPERATION_ABORTED);
                return null;
            }
            try {
                volunteerService.verifyDefaultCredentials (nickname, defaultPassword);
                break;
            } catch (IllegalArgumentException |  IllegalStateException ex ) {
                printer.println (ERROR_PREFIX + safeMessage (ex));
            }
        }

        while (true) {
            String newNickname = reader.readLine (VOLUNTEER_NEW_NICKNAME_PROMPT);
            if (newNickname == null) return null;

            String newPassword = reader.readLine (VOLUNTEER_NEW_PASSWORD_PROMPT);
            if (newPassword == null) return null;

            String confirmation = reader.readLine (VOLUNTEER_CONFIRM_PASSWORD_PROMPT);
            if (confirmation == null) return null;

            if (!Objects.equals(newNickname, confirmation)) {
                printer.println (ERROR_PREFIX + VOLUNTEER_PASSWORD_MISMATCH);
               continue;
            }

            try {
                volunteerService.setPersonalCredentials (newNickname, newPassword);
                printer.println (VOLUNTEER_FIRST_ACCESS_SUCCESS.formatted (newNickname));
                return newNickname;
            } catch (IllegalArgumentException |  IllegalStateException ex ) {
                printer.println (ERROR_PREFIX + safeMessage (ex));
            }

        }
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
                service.setPersonalCredentials(ConfiguratorServiceImp.DEFAULT_NICKNAME, nickname, password);
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
