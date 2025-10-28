package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.*;
import it.unibs.ingsw24_25.cli.Printer;
import it.unibs.ingsw24_25.cli.PromptReader;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.ConfiguratorServiceImp;
import it.unibs.ingsw24_25.util.AvailabilitySubmissionPolicy;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ConfiguratorCommandHandler {
    private static final String MENU_SEPARATOR = "-----------------------";
    private static final String DEFAULT_PROMPT = "> ";
    private static final String BACK_COMMAND = "back";
    private static final String INVALID_COMMAND_MESSAGE = "Comando non valido";
    private static final String ERROR_PREFIX = "[ERRORE] : ";
    private static final String OPERATION_ABORTED = "Operazione annullata.";
    private static final String INVALID_NUMBER_MESSAGE = "Inserire un numero intero valido.";
    private static final String PRECLUDED_DATES_INVALID_FORMAT = "Formato data non valido: %s. Usa yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern ("HH:mm");
    private static final DateTimeFormatter YEAR_MONTH_INPUT_FORMATTER = DateTimeFormatter.ofPattern("MM-yyyy");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern ("MM yyyy", Locale.ITALIAN);

    private static final String CONFIGURATOR_LOGIN_HEADER = "Autenticazione configuratore";
    private static final String CONFIGURATOR_LOGIN_NICK_PROMPT = "Nickname configuratore (\"back\" per annullare): ";
    private static final String CONFIGURATOR_LOGIN_PASSWORD_PROMPT = "Password: ";
    private static final String CONFIGURATOR_LOGIN_FAILURE = "Credenziali non valide. Riprova.";
    private static final String CONFIGURATOR_LOGIN_SUCCESS = "Accesso effettuato.";

    private static final String CONFIGURATOR_MENU_HEADER = "Menu configuratore";
    private static final String CONFIGURATOR_MENU_SETUP_OPTION = "1 - Menu setup";
    private static final String CONFIGURATOR_MENU_LIST_OPTION = "2 - Menu liste";
    private static final String CONFIGURATOR_MENU_SETTINGS_OPTION = "3 - Menu impostazioni";
    private static final String CONFIGURATOR_MENU_PLANNING_OPTION = "4 - Menu pianificazione";
    private static final String CONFIGURATOR_MENU_BACK_OPTION = "back - Torna alla schermata precedente";
    private static final String CONFIGURATOR_MENU_LOGOUT_OPTION = "logout - Esci dal profilo";

    private static final String PLANNING_MENU_HEADER = "Menu pianificazione";
    private static final String PLANNING_CLOSE_WINDOW_OPTION = "1 - Chiudi finestra disponibilità";
    private static final String PLANNING_GENERATE_PLAN_OPTION = "2 - Genera piano mensile";
    private static final String PLANNING_ASSIGN_OPTION = "3 - Assegna volontario a visita";
    private static final String PLANNING_REMOVE_VISIT_OPTION = "4 - Rimuovi visita pianificata";
    private static final String PLANNING_REMOVE_PLACE_OPTION = "5 - Rimuovi luogo (con cascata)";
    private static final String PLANNING_REMOVE_VISIT_TYPE_OPTION = "6 - Rimuovi tipo visita (con cascata)";
    private static final String PLANNING_REMOVE_VOLUNTEER_OPTION = "7 - Rimuovi volontario";
    private static final String PLANNING_REOPEN_OPTION = "8 - Riapri finestra disponibilità";

    private static final String PLANNING_DATE_PROMPT = "Inserisci la data (yyyy-MM-dd): ";
    private static final String PLANNING_MONTH_PROMPT = "Inserisci il mese di pianificazione (MM-yyyy): ";
    private static final String PLANNING_VISIT_TYPE_PROMPT = "Identificativo (ID) del tipo visita: ";
    private static final String PLANNING_VOLUNTEER_PROMPT = "Nickname volontario da assegnare: ";
    private static final String PLANNING_CONFIRM_PROMPT = "Confermi? (si/no): ";
    private static final String PLANNING_WARNING_PLACE = "Attenzione: la rimozione del luogo eliminerà visite pianificate e assegnazioni collegate.";
    private static final String PLANNING_WARNING_VISIT_TYPE = "Attenzione: la rimozione del tipo visita eliminerà visite pianificate e potenzialmente volontari rimasti senza incarichi.";
    private static final String PLANNING_WARNING_VOLUNTEER = "Attenzione: la rimozione del volontario eliminerà le sue disponibilità e i turni assegnati.";
    private static final String PLANNING_SUCCESS_CLOSE = "Finestra di disponibilità chiusa.";
    private static final String PLANNING_SUCCESS_GENERATION = "Piano mensile generato.";
    private static final String PLANNING_SUCCESS_ASSIGNMENT = "Assegnazione completata.";
    private static final String PLANNING_SUCCESS_VISIT_REMOVAL = "Visita pianificata rimossa.";
    private static final String PLANNING_SUCCESS_PLACE_REMOVAL = "Luogo rimosso.";
    private static final String PLANNING_SUCCESS_VISIT_TYPE_REMOVAL = "Tipo visita rimosso.";
    private static final String PLANNING_SUCCESS_VOLUNTEER_REMOVAL = "Volontario rimosso.";
    private static final String PLANNING_SUCCESS_REOPEN = "Finestra di disponibilità riaperta.";
    private static final String PLANNING_NO_PLAN_MESSAGE = "Nessun piano disponibile per il mese richiesto.";

    private static final String PLACE_SELECTION_PROMPT = "Inserisci il titolo del luogo di cui visualizzare le visite: ";
    private static final String PLACE_NAME_PROMPT = "Inserisci il nome del luogo: ";
    private static final String PLACE_DESCRIPTION_PROMPT = "Inserisci la descrizione del luogo: ";
    private static final String PLACE_LOCATION_PROMPT = "Inserisci la localizzazione del luogo: ";
    private static final String PLACE_REMOVE_PROMPT = "Inserisci il nome del luogo da rimuovere: ";

    private static final String VISIT_PLACE_PROMPT = "Inserisci nome del luogo relativo alla visita: ";
    private static final String VISIT_TITLE_PROMPT = "Inserisci il titolo della visita: ";
    private static final String VISIT_DESCRIPTION_PROMPT = "Inserisci la descrizione della visita: ";
    private static final String VISIT_MEETING_PROMPT = "Inserisci il luogo di ritrovo: ";
    private static final String VISIT_DAY_PROMPT = "Giorno della settimana (1=Lunedì ... 7=Domenica): ";
    private static final String VISIT_START_PROMPT = "Orario di inizio (HH:mm): ";
    private static final String VISIT_DURATION_PROMPT = "Durata in minuti: ";
    private static final String VISIT_ANOTHER_SLOT_PROMPT = "Aggiungere un altro orario? (si/no): ";
    private static final String VISIT_TICKET_PROMPT = "La visita richiede un ticket? (si/no): ";
    private static final String VISIT_MIN_PROMPT = "Numero minimo di partecipanti: ";
    private static final String VISIT_MAX_PROMPT = "Numero massimo di partecipanti: ";
    private static final String VISIT_VALID_FROM_PROMPT = "Data di inizio validità (yyyy-MM-dd): ";
    private static final String VISIT_VALID_TO_PROMPT = "Data di fine validità (yyyy-MM-dd): ";
    private static final String VISIT_INVALID_DATE_RANGE = "La data di fine non può essere precedente alla data di inizio.";
    private static final String VISIT_REMOVE_PROMPT = "Identificativo (ID) del tipo visita da rimuovere: ";


    private static final String VOLUNTEER_NICKNAME_PROMPT = "Inserisci il nickname del volontario: ";
    private static final String VOLUNTEER_PASSWORD_PROMPT = "Inserisci la passowrd iniziale del volontario: ";
    private static final String VOLUNTEER_SUCCESS = "Volontario inserito con successo.";
    private static final String VOLUNTEER_LINK_VISIT_PROMPT = "Inserisci l'ID del tipo visita da associare: ";
    private static final String VOLUNTEER_LINK_SUCCESS = "Associazione completata.";

    private static final String VOLUNTEER_REMOVE_PROMPT = "Nickname del volontario da rimuovere: ";
    private static final String PRECLUDED_DATES_SUCCESS = "Date precluse aggiornate.";
    private static final String CLEAR_TOKEN = "-";
    private static final String CLEAR_TOKEN_ALT = "nessuna";
    private static final String MAX_PEOPLE_PROMPT = "Inserisci il numero massimo di persone per iscrizione: ";
    private static final String MAX_PEOPLE_SUCCESS = "Numero massimo di persone per iscrizione aggiornato.";

    private final ConfiguratorService service;
    private final Printer printer;
    private final PromptReader reader;

    public ConfiguratorCommandHandler(ConfiguratorService service,
                                      Printer printer,
                                      PromptReader reader) {
        this.service = Objects.requireNonNull(service);
        this.printer = Objects.requireNonNull(printer);
        this.reader = Objects.requireNonNull(reader);
    }

    public void startSession(){
        if (!authenticateCOnfigurator()) return;

        runConfiguratorMenu();

    }

    private boolean authenticateCOnfigurator() {
        if (service.isFirstAccessPending (ConfiguratorServiceImp.DEFAULT_NICKNAME)) return true;

        printer.println (MENU_SEPARATOR);
        printer.println (CONFIGURATOR_LOGIN_HEADER);
        printer.println (MENU_SEPARATOR);

        while (true) {
            String nickname = reader.readLine (CONFIGURATOR_LOGIN_NICK_PROMPT);
            if (isBackCommand(nickname)){
                printer.println (OPERATION_ABORTED);
                return false;
            }

            String password = reader.readLine (CONFIGURATOR_LOGIN_PASSWORD_PROMPT);
            if (isBackCommand(password)){
                printer.println (OPERATION_ABORTED);
                return false;
            }

            if (service.verifyLogin (nickname, password)) {
                printer.println (CONFIGURATOR_LOGIN_SUCCESS);
                return true;
            }

            printer.println (CONFIGURATOR_LOGIN_FAILURE);
        }
    }

    private void runConfiguratorMenu() {
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println(CONFIGURATOR_MENU_HEADER);
            printer.println(CONFIGURATOR_MENU_SETUP_OPTION);
            printer.println(CONFIGURATOR_MENU_LIST_OPTION);
            printer.println(CONFIGURATOR_MENU_SETTINGS_OPTION);
            printer.println (CONFIGURATOR_MENU_PLANNING_OPTION);
            printer.println(CONFIGURATOR_MENU_LOGOUT_OPTION);
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine (DEFAULT_PROMPT);
            if (choice == null) {
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }

            String normalized = choice.trim ().toLowerCase (Locale.ITALIAN);
            switch (normalized) {
                case "1", "setup" -> openSetupMenu();
                case "2", "list" -> openListMenu();
                case "3", "settings" -> openSettingsMenu();
                case "4", "planning" -> openPlanningMenu();
                case BACK_COMMAND ->  stayInMenu = false;
                default -> printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
    }

    public boolean openSetupMenu() {
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println("Menu setup: ");
            printer.println("1 - Aggiungi luogo");
            printer.println("2 - Aggiungi visita");
            printer.println("3 - aggiungi volontario");
            printer.println("4 - Associa volontario a visita");
            printer.println(CONFIGURATOR_MENU_BACK_OPTION);
            printer.println("Menu setup: ");

            String choice = reader.readLine(DEFAULT_PROMPT).toLowerCase(Locale.ITALIAN);
            switch (choice) {
                case "1" -> addPlace();
                case "2" -> addVisitType();
                case "3" -> addVolunteer();
                case "4" -> linkVolunteerToVisit();
                case BACK_COMMAND -> stayInMenu = false;
                default -> printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
        return true;
    }
    public boolean openListMenu() {
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println("Menu liste");
            printer.println("1 - Visualizza luoghi");
            printer.println("2 - Visualizza visite per luogo");
            printer.println("3 - Visualizza visite con stato");
            printer.println("4 - Visualizza volontari");
            printer.println(CONFIGURATOR_MENU_BACK_OPTION);
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine(DEFAULT_PROMPT).toLowerCase(Locale.ITALIAN);
            switch (choice) {
                case "1" -> listPlaces();
                case "2" -> listVisitTypeByPlace();
                case "3" -> listVisistTypeWithState();
                case "4" -> listVolunteer();
                case BACK_COMMAND -> stayInMenu = false;
                default -> printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
        return true;
    }
    public boolean openSettingsMenu() {
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println("Menu impostazioni");
            printer.println("1 - Imposta date precluse");
            printer.println("2 - Imposta max persone per iscrizione");
            printer.println(CONFIGURATOR_MENU_BACK_OPTION);
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine(DEFAULT_PROMPT).toLowerCase(Locale.ITALIAN);
            switch (choice) {
                case "1" -> setBlackOutDates();
                case "2" -> setMaxPeoplePerSub();
                case BACK_COMMAND -> stayInMenu = false;
                default -> printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
        return true;
    }
    public boolean openPlanningMenu() {
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println(PLANNING_MENU_HEADER);
            printer.println(PLANNING_CLOSE_WINDOW_OPTION);
            printer.println(PLANNING_GENERATE_PLAN_OPTION);
            printer.println(PLANNING_ASSIGN_OPTION);
            printer.println(PLANNING_REMOVE_VISIT_OPTION);
            printer.println(PLANNING_REMOVE_PLACE_OPTION);
            printer.println(PLANNING_REMOVE_VISIT_TYPE_OPTION);
            printer.println(PLANNING_REMOVE_VOLUNTEER_OPTION);
            printer.println(PLANNING_REOPEN_OPTION);
            printer.println("back - Torna al menu principale");
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine(DEFAULT_PROMPT).toLowerCase(Locale.ITALIAN);
            switch (choice) {
                case "1" -> closeAvailabilityWindowFlow();
                case "2" -> generatePlanFlow();
                case "3" -> assignVolunteerToPlannedVisitFlow();
                case "4" -> removePlannedVisitFlow();
                case "5" -> removePlaceCascadeFlow();
                case "6" -> removeVisitTypeCascadeFlow();
                case "7" -> removeVolunteerFlow();
                case "8" -> reopenAvailabilityWindowFlow();
                case BACK_COMMAND -> stayInMenu = false;
                default -> printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
        return true;
    }

    //setup methods
    private void addPlace() {
        try {
            String title = Objects.requireNonNull (reader.readLine (PLACE_NAME_PROMPT));
            String description = Objects.requireNonNull(reader.readLine (PLACE_DESCRIPTION_PROMPT));
            String location = Objects.requireNonNull(reader.readLine (PLACE_LOCATION_PROMPT));
            String result = service.addPlace(title, description, location);
            printer.println (result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }
    private void addVisitType() {
        try {
            String placeId = Objects.requireNonNull (reader.readLine (VISIT_PLACE_PROMPT));
            String title = Objects.requireNonNull (reader.readLine (VISIT_TITLE_PROMPT));
            String description = Objects.requireNonNull(reader.readLine (VISIT_DESCRIPTION_PROMPT));
            String meetingLocation = Objects.requireNonNull (reader.readLine (VISIT_MEETING_PROMPT));
            List<TimeSlot> schedules = readVisitSchedules();
            boolean ticketRequired = reader.readBoolean (VISIT_TICKET_PROMPT);
            int minParticipants = readPositiveInt(VISIT_MIN_PROMPT);
            int maxParticipants = readMaxParticipants(minParticipants);
            LocalDate validFrom = readDate(VISIT_VALID_FROM_PROMPT);
            LocalDate validTo = readDate(VISIT_VALID_TO_PROMPT);
            if (validTo.isBefore(validFrom)) {
                printer.println (ERROR_PREFIX + VISIT_INVALID_DATE_RANGE);
                return;
            }

            String result = service.addVisitType (
                    placeId,
                    title,
                    description,
                    meetingLocation,
                    schedules,
                    ticketRequired,
                    minParticipants,
                    maxParticipants,
                    validFrom,
                    validTo
            );
            printer.println (result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }
    private void addVolunteer() {
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            String password = Objects.requireNonNull(reader.readLine (VOLUNTEER_PASSWORD_PROMPT));
            service.addVolunteer(nickname, password);
            printer.println (VOLUNTEER_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }
    private void linkVolunteerToVisit() {
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            String visitId = Objects.requireNonNull (reader.readLine (VOLUNTEER_LINK_VISIT_PROMPT));
            service.linkVOlunteerToVisit (nickname, visitId);
            printer.println (VOLUNTEER_LINK_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }

    //list methods
    private void listPlaces() {
        printer.println(Printer.PLACE_HEADER);
        List<PlaceDTO> places = service.listPlace ();
        printer.printPlaceList (places);
    }
    private void listVisitTypeByPlace() {
        List<PlaceDTO> places = service.listPlace ();
        printer.println(Printer.PLACE_HEADER);
        printer.printPlaceList (places);
        if (places == null || places.isEmpty()) {
            return;
        }
        String placeId = reader.readLine (PLACE_SELECTION_PROMPT);
        try {
            List<VisitTypeDTO> visitTypes = service.listVisitTypeByPlace (placeId);
            printer.println (Printer.VISIT_TYPE_HEADER);
            printer.printVisitTypeList (visitTypes);
        } catch (IllegalArgumentException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }
    private void listVisistTypeWithState() {
        try {
            List<VisitTypeDTO> visitTypes = service.listVisitType ();
            printer.println (Printer.VISIT_TYPE_HEADER);
            printer.printVisitTypeList (visitTypes);
        } catch (IllegalArgumentException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }
    private void listVolunteer() {
        printer.println(Printer.VOLUNTEER_HEADER);
        List<VolunteerDTO> volunteers = service.listVolunteerWVisitType ();
        printer.printVolunteerList (volunteers);
    }

    //settings methods
    private void setBlackOutDates() {
        List<String> rawDates = reader.readValues (buildExcludedDatesPrompt(), null);
        if (rawDates.size () == 1) {
            String token = rawDates.get (0).toLowerCase (Locale.ITALIAN);
            if (CLEAR_TOKEN.equals(token) || CLEAR_TOKEN.equals(token)) {
                rawDates = List.of();
            }
        }

        if (rawDates.isEmpty()) {
            try {
                service.setBlackoutDates (List.of());
                printer.println (PRECLUDED_DATES_SUCCESS);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                printer.println (ERROR_PREFIX + safeMessage(ex));
            }
            return;
        }

        List<LocalDate> dates = new ArrayList<> ();
        for (String date : rawDates) {
            try {
                dates.add (LocalDate.parse (date, DATE_FORMATTER));
            } catch (DateTimeParseException ex) {
                printer.println (ERROR_PREFIX + PRECLUDED_DATES_INVALID_FORMAT.formatted (date));
                return;
            }
        }
        try {
            service.setBlackoutDates (dates);
            printer.println (PRECLUDED_DATES_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void setMaxPeoplePerSub() {
        String value = reader.readLine (MAX_PEOPLE_PROMPT);
        int max;
        try {
            max = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            printer.println (ERROR_PREFIX + INVALID_NUMBER_MESSAGE);
            return;
        }

        try {
            service.setMaxPeoplePerSubscription(max);
            printer.println (MAX_PEOPLE_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    //planning methods
    private void closeAvailabilityWindowFlow() {
        LocalDate today = readDate (PLANNING_DATE_PROMPT);
        try {
            service.closeAvailabilityWindow (today);
            printer.println (PLANNING_SUCCESS_CLOSE);
            YearMonth targetMonth = AvailabilitySubmissionPolicy.nextSubmissionMonth (today);
            displayAvailabilitySnapshots(targetMonth);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void generatePlanFlow(){
        YearMonth month = readYearMonth(PLANNING_MONTH_PROMPT);
        try {
            MonthlyPlanDTO plan = service.generateMonthlyPlan (month);
            printer.println (PLANNING_SUCCESS_GENERATION);
            printer.printMonthlyPlan(plan);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void assignVolunteerToPlannedVisitFlow() {
        YearMonth month = readYearMonth(PLANNING_MONTH_PROMPT);
        LocalDate date = readDate (PLANNING_DATE_PROMPT);
        LocalTime startTime = readTime (VISIT_START_PROMPT);
        int durationMinutes = readPositiveInt (VISIT_DURATION_PROMPT);
        TimeSlot slot = new TimeSlot (date.getDayOfWeek (), startTime, Duration.ofMinutes(durationMinutes));
        String visitTypeId = reader.readLine (PLANNING_VISIT_TYPE_PROMPT);
        String volunteerId = reader.readLine (PLANNING_VOLUNTEER_PROMPT);
        try {
            service.assignVolunteerToPlannedVisit (month, date, slot, visitTypeId, volunteerId);
            printer.println (PLANNING_SUCCESS_ASSIGNMENT);
            displayPlan(month);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void removePlannedVisitFlow() {
        YearMonth month = readYearMonth(PLANNING_MONTH_PROMPT);
        LocalDate date = readDate(PLANNING_DATE_PROMPT);
        LocalTime startTime = readTime(VISIT_START_PROMPT);
        int durationMinutes = readPositiveInt(VISIT_DURATION_PROMPT);
        TimeSlot slot = new TimeSlot(date.getDayOfWeek(), startTime, Duration.ofMinutes(durationMinutes));
        String visitTypeId = reader.readLine(PLANNING_VISIT_TYPE_PROMPT);
        try {
            service.removePlannedVisit(month, date, slot, visitTypeId);
            printer.println(PLANNING_SUCCESS_VISIT_REMOVAL);
            displayPlan(month);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }


    private void removePlaceCascadeFlow(){
        String placeId = reader.readLine (PLACE_REMOVE_PROMPT);
        if (!confirmCascade(PLANNING_WARNING_PLACE)) {
            printer.println (OPERATION_ABORTED);
            return;
        }
        try {
            service.removePlace (placeId);
            printer.println (PLANNING_SUCCESS_PLACE_REMOVAL);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void removeVisitTypeCascadeFlow() {
        String visitTypeId = reader.readLine (VISIT_REMOVE_PROMPT);
        if (!confirmCascade(PLANNING_WARNING_VISIT_TYPE)) {
            printer.println(OPERATION_ABORTED);
            return;
        }
        try {
            service.removeVisitType (visitTypeId);
            printer.println (PLANNING_SUCCESS_VISIT_TYPE_REMOVAL);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void removeVolunteerFlow() {
        String volunteer = reader.readLine (VOLUNTEER_REMOVE_PROMPT);
        if (!confirmCascade(PLANNING_WARNING_VOLUNTEER)) {
            printer.println(OPERATION_ABORTED);
            return;
        }
        try {
            service.removeVolunteer (volunteer);
            printer.println (PLANNING_SUCCESS_VOLUNTEER_REMOVAL);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void reopenAvailabilityWindowFlow(){
        LocalDate today = readDate (PLANNING_DATE_PROMPT);
        try{
            service.reopenAvailabilityWindow (today);
            printer.println (PLANNING_SUCCESS_REOPEN);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    //util methods
    private String buildExcludedDatesPrompt() {
        YearMonth allowedMonth = ExcludedDatePolicy.allowedMonth (LocalDate.now ());
        String monthLabel = allowedMonth.format (YEAR_MONTH_FORMATTER);
        return "Inserisci le date precluse per " + monthLabel +
                "separate da virgola (formato gg-mm-yyyy). Digita '-'/'nessuna' per non impostarne";
    }

    private List<TimeSlot> readVisitSchedules() {
        List<TimeSlot> schedules = new ArrayList<>();
        printer.println("Configura gli orari della visita");
        boolean addMore;
        do {
            DayOfWeek day = readDayOfWeek();
            LocalTime startTime = readTime(VISIT_START_PROMPT);
            int durationMinutes = readPositiveInt(VISIT_DURATION_PROMPT);
            schedules.add(new TimeSlot(day, startTime, Duration.ofMinutes(durationMinutes)));
            addMore = reader.readBoolean(VISIT_ANOTHER_SLOT_PROMPT);
        } while (addMore);
        return schedules;
    }

    private DayOfWeek readDayOfWeek() {
        while (true) {
            String value = reader.readLine(VISIT_DAY_PROMPT);
            try {
                int numeric = Integer.parseInt(value);
                if (numeric >= 1 && numeric <= 7) {
                    return DayOfWeek.of(numeric);
                }
            } catch (NumberFormatException ignored) {
            }

            String normalized = value.trim().toLowerCase(Locale.ITALIAN).replace('ì', 'i');
            switch (normalized) {
                case "lunedi", "lun", "monday" -> {
                    return DayOfWeek.MONDAY;
                }
                case "martedi", "mar", "tuesday" -> {
                    return DayOfWeek.TUESDAY;
                }
                case "mercoledi", "mer", "wednesday" -> {
                    return DayOfWeek.WEDNESDAY;
                }
                case "giovedi", "gio", "thursday" -> {
                    return DayOfWeek.THURSDAY;
                }
                case "venerdi", "ven", "friday" -> {
                    return DayOfWeek.FRIDAY;
                }
                case "sabato", "sab", "saturday" -> {
                    return DayOfWeek.SATURDAY;
                }
                case "domenica", "dom", "sunday" -> {
                    return DayOfWeek.SUNDAY;
                }
                default -> printer.println(ERROR_PREFIX + "Giorno non valido. Usa un numero tra 1 e 7 o il nome del giorno.");
            }
        }
    }

    private LocalTime readTime(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            try {
                return LocalTime.parse(value, TIME_FORMATTER);
            } catch (DateTimeParseException ex) {
                printer.println(ERROR_PREFIX + "Fromato orario non valido. Usa HH:mm");
            }
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ex) {
            }

            printer.println(ERROR_PREFIX + INVALID_NUMBER_MESSAGE);
        }
    }

    private int readMaxParticipants(int minParticipants) {
        while (true) {
            int maxParticipants = readPositiveInt(VISIT_MAX_PROMPT);
            if (maxParticipants >= minParticipants) {
                return maxParticipants;
            }
            printer.println(ERROR_PREFIX + "Il numero massimo di iscrizioni deve essere maggiore o uguale al minimo indicato");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            try {
                return LocalDate.parse(value, DATE_FORMATTER);
            } catch (DateTimeParseException ex) {
                printer.println(ERROR_PREFIX + PRECLUDED_DATES_INVALID_FORMAT.formatted(value));
            }
        }
    }
    private YearMonth readYearMonth(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            try {
                return YearMonth.parse(value, YEAR_MONTH_INPUT_FORMATTER);
            } catch (DateTimeParseException ex) {
                printer.println(ERROR_PREFIX + "Formato mese non valido. Usa MM-yyyy");
            }
        }
    }


    private boolean isBackCommand(String value) {
        return value != null && value.trim ().equalsIgnoreCase (BACK_COMMAND);
    }
    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? "" : message;
    }

    private void displayAvailabilitySnapshots(YearMonth month) {
        try {
            MonthlyPlanDTO plan = service.getMonthlyPlanDetails(month);
            List<PlanAvailabilityDTO> snapshots = plan.getAvailabilitySnapshots();
            if (snapshots == null || snapshots.isEmpty()) {
                printer.println("Nessuna disponibilità registrata per il mese " + month + ".");
                return;
            }
            printer.printAvailabilitySnapshots(snapshots);
        } catch (IllegalStateException ex) {
            printer.println(PLANNING_NO_PLAN_MESSAGE);
        } catch (RuntimeException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void displayPlan(YearMonth month) {
        try {
            MonthlyPlanDTO plan = service.getMonthlyPlanDetails(month);
            printer.printMonthlyPlan(plan);
        } catch (IllegalStateException ex) {
            printer.println(PLANNING_NO_PLAN_MESSAGE);
        } catch (RuntimeException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }

    private boolean confirmCascade(String warningMessage) {
        printer.println(warningMessage);
        return reader.readBoolean(PLANNING_CONFIRM_PROMPT);
    }



}