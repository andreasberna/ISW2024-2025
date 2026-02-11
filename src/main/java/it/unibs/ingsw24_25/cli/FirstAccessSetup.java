package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.service.FirstAccessService;

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
    private static final String VOLUNTEER_NEW_PASSWORD_PROMPT = "Imposta una nuova password: ";
    private static final String VOLUNTEER_CONFIRM_PASSWORD_PROMPT = "Conferma la nuova password: ";
    private static final String VOLUNTEER_PASSWORD_MISMATCH = "Le password non coincidono.";
    private static final String VOLUNTEER_FIRST_ACCESS_SUCCESS = "Credenziali personali impostate con successo. Nuovo nickname: %s.";
    private static final String OPERATION_ABORTED = "Operazione annullata.";

    private final FirstAccessService firstAccessService;
    private final PromptReader reader;
    private final Printer printer;

    public FirstAccessSetup(FirstAccessService firstAccessService,
                            PromptReader reader, Printer printer) {
        this.firstAccessService = Objects.requireNonNull(firstAccessService, "service non può essere nullo");
        this.reader = Objects.requireNonNull(reader, "Reader non può essere nulla");
        this.printer = Objects.requireNonNull(printer, "Printer non può essere nulla");
    }

    public void run(){
        firstAccessService.runConfiguratorFirstAccess (buildConfiguratorView());
    }

    public String handleVolunteerFirstAccess(String nickname){
       return firstAccessService.runVolunteerFirstAccess (nickname, buildVolunteerView());
    }

    private FirstAccessService.ConfiguratorFirstAccessView buildConfiguratorView(){
        return new FirstAccessService.ConfiguratorFirstAccessView () {
            @Override
            public void showSeparator() {
                printer.println(MENU_SEPARATOR);
            }

            @Override
            public void showIntro() {
                printer.println(INTRO_MESSAGE);
            }

            @Override
            public void showCompletion() {
                printer.println(COMPLETION_MESSAGE);
            }

            @Override
            public String readDefaultNickname() {
                return reader.readLine (DEFAULT_NICK_PROMPT);
            }

            @Override
            public String readDefaultPassword() {
                return reader.readLine (DEFAULT_PASS_PROMPT);
            }

            @Override
            public void showDefaultCredentialsSuccess() {
                printer.print(DEFAULT_CREDENTIALS_SUCCESS);
            }

            @Override
            public String readPersonalNickname() {
                return reader.readLine (PERSONAL_NICK_PROMPT);
            }

            @Override
            public String readPersonalPassword() {
                return reader.readLine (PERSONAL_PASS_PROMPT);
            }

            @Override
            public void showPersonalCredentialsSuccess() {
                printer.print(PERSONAL_CREDENTIALS_SUCCESS);
            }

            @Override
            public String readTerritorialScope() {
                return reader.readLine (TERRITORIAL_SCOPE_PROMPT);
            }

            @Override
            public String readMaxPeople() {
                return reader.readLine (MAX_PEOPLE_PROMPT);
            }

            @Override
            public void showError(String message) {
                printer.println (ERROR_PREFIX + message);
            }
        };
    }

    private FirstAccessService.VolunteerFirstAccessView buildVolunteerView(){
        return new FirstAccessService.VolunteerFirstAccessView () {
            @Override
            public void showSeparator() {
                printer.print(MENU_SEPARATOR);
            }

            @Override
            public void showVolunteerHeader() {
                printer.print(VOLUNTEER_FIRST_ACCESS_HEADER);
            }

            @Override
            public String readVolunteerDefaultPassword() {
                return reader.readLine (VOLUNTEER_DEFAULT_PASSWORD_PROMPT);
            }

            @Override
            public void showOperationAborted() {
                printer.print(OPERATION_ABORTED);
            }

            @Override
            public String readVolunteerNewNickname() {
                return reader.readLine (VOLUNTEER_NEW_NICKNAME_PROMPT);
            }

            @Override
            public String readVolunteerNewPassword() {
                return reader.readLine (VOLUNTEER_NEW_PASSWORD_PROMPT);
            }

            @Override
            public String readVolunteerConfirmPassword() {
                return reader.readLine (VOLUNTEER_CONFIRM_PASSWORD_PROMPT);
            }

            @Override
            public void showPasswordMismatch() {
                printer.println(ERROR_PREFIX + VOLUNTEER_PASSWORD_MISMATCH);
            }

            @Override
            public void showVolunteerSuccess(String newNickname) {
                printer.println (VOLUNTEER_FIRST_ACCESS_SUCCESS.formatted (newNickname));
            }

            @Override
            public void showError(String message) {
                printer.println (ERROR_PREFIX + message);
            }
        };
    }
}
