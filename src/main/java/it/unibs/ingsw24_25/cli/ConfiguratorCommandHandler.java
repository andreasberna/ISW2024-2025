package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.AssignedShiftDTO;
import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.AssignedShift;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.ConfiguratorServiceImp;
import it.unibs.ingsw24_25.service.VolunteerService;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CommandHandler {
    private static final String MENU_SEPARATOR = "-----------------------";
    private static final String DEFAULT_PROMPT = "> ";
    private static final String BACK_COMMAND = "back";

    private static final String INVALID_COMMAND_MESSAGE = "Comando non valido";
    private static final String ERROR_PREFIX = "Errore: ";

    private static final String CONFIGURATOR_LOGIN_HEADER = "Autenticazione configuratore";
    private static final String CONFIGURATOR_LOGIN_NICK_PROMPT = "Nickname configuratore (\"back\" per annullare): ";
    private static final String CONFIGURATOR_LOGIN_PASSWORD_PROMPT = "Password: ";
    private static final String CONFIGURATOR_LOGIN_FAILURE = "Credenziali non valide. Riprova.";
    private static final String CONFIGURATOR_LOGIN_SUCCESS = "Accesso effettuato.";
    private static final String CONFIGURATOR_MENU_HEADER = "Menu configuratore";
    private static final String CONFIGURATOR_MENU_SETUP_OPTION = "1 - Menu setup";
    private static final String CONFIGURATOR_MENU_LIST_OPTION = "2 - Menu liste";
    private static final String CONFIGURATOR_MENU_SETTINGS_OPTION = "3 - Menu impostazioni";
    private static final String CONFIGURATOR_MENU_VOLUNTEER_OPTION = "4 - Gestione volontari";
    private static final String CONFIGURATOR_MENU_BACK_OPTION = "back - Torna alla schermata iniziale";

    private static final String VOLUNTEER_ENTRY_HEADER = "Accesso volontario";
    private static final String VOLUNTEER_LOGIN_PROMPT = "Nickname volontario (\"back\" per annullare): ";
    private static final String VOLUNTEER_LOGIN_PASSWORD_PROMPT = "Password: ";
    private static final String VOLUNTEER_LOGIN_FAILURE = "Credenziali non valide. Riprova.";
    private static final String VOLUNTEER_LOGIN_SUCCESS = "Accesso effettuato.";
    private static final String VOLUNTEER_FIRST_ACCESS_HEADER = "Primo accesso volontario";
    private static final String VOLUNTEER_DEFAULT_PASSWORD_PROMPT = "Inserisci la password temporanea (\"back\" per annullare): ";
    private static final String VOLUNTEER_NEW_PASSWORD_PROMPT = "Imposta una nuova password: ";
    private static final String VOLUNTEER_CONFIRM_PASSWORD_PROMPT = "Conferma la nuova password: ";
    private static final String VOLUNTEER_PASSWORD_MISMATCH = "Le password non coincidono.";
    private static final String VOLUNTEER_FIRST_ACCESS_SUCCESS = "Credenziali personali impostate con successo.";
    private static final String VOLUNTEER_SELF_MENU_HEADER = "Menu volontario";
    private static final String VOLUNTEER_SELF_MENU_SUBMIT_OPTION = "1 - Invia disponibilità mensile";
    private static final String VOLUNTEER_SELF_MENU_AVAILABILITY_OPTION = "2 - Visualizza disponibilità mensile";
    private static final String VOLUNTEER_SELF_MENU_SHIFTS_OPTION = "3 - Visualizza turni assegnati";
    private static final String VOLUNTEER_SELF_MENU_BACK_OPTION = "back - Esci";
    private static final String OPERATION_ABORTED = "Operazione annullata.";
    private static final String VOLUNTEER_PREFERRED_DAYS_PROMPT = "Giorni disponibili (es. 1,3,5 o lun,mer,ven). Digita \"back\" per annullare: ";
    private static final String VOLUNTEER_WEEKLY_FREQUENCY_PROMPT = "Quante disponibilità settimanali puoi garantire? ";
    private static final String VOLUNTEER_AVAILABILITY_SUCCESS = "Disponibilità registrata correttamente.";
    private static final String VOLUNTEER_INVALID_DAY_INPUT = "Inserire almeno un giorno valido della settimana.";
    private static final String VOLUNTEER_UNKNOWN_DAY = "Giorno non riconosciuto: %s.";

    private static final String PLACE_SELECTION_PROMPT = "Inserisci il titolo del luogo di cui visualizzare le visite: ";
    private static final String PLACE_NAME_PROMPT = "Inserisci il nome del luogo: ";
    private static final String PLACE_DESCRIPTION_PROMPT = "Inserisci la descrizione del luogo: ";
    private static final String PLACE_LOCATION_PROMPT = "Inserisci la localizzazione del luogo: ";

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

    private static final String VOLUNTEER_NICKNAME_PROMPT = "Inserisci il nickname del volontario: ";
    private static final String VOLUNTEER_PASSWORD_PROMPT = "Inserisci la passowrd iniziale del volontario: ";
    private static final String VOLUNTEER_SUCCESS = "Volontario inserito con successo.";
    private static final String VOLUNTEER_LINK_VISIT_PROMPT = "Inserisci il titolo della visita da associare: ";
    private static final String VOLUNTEER_LINK_SUCCESS = "Associazione completata.";
    private static final String VOLUNTEER_MONTH_PROMPT = "Inserisci il mese di riferimento (yyyy-MM): ";
    private static final String VOLUNTEER_AVAILABILITY_HEADER = "Disponibilità registrata per %s nel mese %s:";
    private static final String VOLUNTEER_NO_AVAILABILITY = "Nessuna disponibilità registrata per %s nel mese %s.";
    private static final String VOLUNTEER_SCHEDULE_HEADER = "Turni assegnati a %s nel mese %s:";
    private static final String VOLUNTEER_NO_SHIFTS = "Nessun turno assegnato a %s per il mese %s.";
    private static final String VOLUNTEER_ASSIGN_PROMPT = "Vuoi inserire un turno per il mese selezionato? (si/no): ";
    private static final String VOLUNTEER_ANOTHER_SHIFT_PROMPT = "Aggiungere un altro turno? (si/no): ";
    private static final String VOLUNTEER_SHIFT_DATE_PROMPT = "Inserisci la data del turno (yyyy-MM-dd): ";
    private static final String VOLUNTEER_SHIFT_VISIT_PROMPT = "Inserisci il titolo della visita del turno: ";
    private static final String VOLUNTEER_SHIFT_START_PROMPT = "Inserisci l'orario di inizio del turno (HH:mm): ";
    private static final String VOLUNTEER_SHIFT_DURATION_PROMPT = "Inserisci la durata del turno in minuti: ";
    private static final String VOLUNTEER_SHIFT_SUCCESS = "Turni aggiornati correttamente.";
    private static final String VOLUNTEER_SHIFT_MONTH_ERROR = "La data deve appartenere al mese %s.";

    private static final String PRECLUDED_DATES_SUCCESS = "Date precluse aggiornate.";
    private static final String PRECLUDED_DATES_INVALID_FORMAT = "Formato data non valido: %s. Usa yyyy-MM-dd";
    private static final String MAX_PEOPLE_PROMPT = "Inserisci il numero massimo di persone per iscrizione: ";
    private static final String MAX_PEOPLE_SUCCESS = "Numero massimo di persone per iscrizione aggiornato.";
    private static final String INVALID_NUMBER_MESSAGE = "Inserire un numero intero valido.";
    private static final String MONTH_INPUT_INVALID = "Formato mese non valido. Usa yyyy-MM.";

    private static final String CLEAR_TOKEN = "-";
    private static final String CLEAR_TOKEN_ALT = "nessuna";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter YEAR_MONTH_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM yyyy", Locale.ITALIAN);

    private final ConfiguratorService service;
    private final VolunteerService volunteerService;
    private final FirstAccessSetup firstAccessSetup;
    private final Printer printer;
    private final PromptReader reader;

    public CommandHandler(ConfiguratorService service,
                          VolunteerService volunteerService,
                          FirstAccessSetup firstAccesSetup,
                          Printer printer, PromptReader prompt) {
        this.service = Objects.requireNonNull(service, "ConfiguratorService non può essere nullo");
        this.volunteerService = Objects.requireNonNull(volunteerService, "VolunteerService non può essere nullo");
        this.firstAccessSetup = Objects.requireNonNull(firstAccesSetup, "FirstAccessSetup non può essere nulla");
        this.printer = Objects.requireNonNull(printer, "Printer non può essere nullo");
        this.reader = Objects.requireNonNull(prompt, "PromptReader non può essere nullo");
    }

    public void startConfiguratorSession() {
        if(!authenticateConfigurator()){
            return;
        }

        runConfiguratorMenu();
    }
    public void startVolunteerSession() {
        while (true) {
            printer.println (MENU_SEPARATOR);
            printer.println (VOLUNTEER_ENTRY_HEADER);
            printer.println (MENU_SEPARATOR);

            String nickname = reader.readLine(VOLUNTEER_LOGIN_PROMPT);
            if (isBackCommand(nickname)) {
                return;
            }

            try {
                String sanitized = Objects.requireNonNull(nickname, "Il nickname del volontario non può essere nullo").trim();
                if (sanitized.isEmpty()) {
                    printer.println(ERROR_PREFIX + "Il nickname del volontario non può essere vuoto.");
                    continue;
                }

                String activeNickname = sanitized;
                if (volunteerService.isFirstAccessPending(sanitized)) {
                    String updatedNickname = firstAccessSetup.handleVolunteerFirstAccess(sanitized);
                    if (updatedNickname == null) {
                        continue;
                    }
                    activeNickname = updatedNickname;
                }

                if (authenticateVolunteer(activeNickname)) {
                    runVolunteerSelfServiceMenu(activeNickname);
                    return;
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                printer.println(ERROR_PREFIX + safeMessage(ex));
            }
        }
    }

    private boolean authenticateConfigurator() {
        if (service.isFirstAccessPending (ConfiguratorServiceImp.DEFAULT_NICKNAME)) return true;

        printer.println (MENU_SEPARATOR);
        printer.println(CONFIGURATOR_LOGIN_HEADER);
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

            if(service.verifyLogin (nickname, password)){
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
            printer.println(CONFIGURATOR_MENU_VOLUNTEER_OPTION);
            printer.println(CONFIGURATOR_MENU_BACK_OPTION);
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine (DEFAULT_PROMPT);
            if(choice == null){
                printer.println (INVALID_COMMAND_MESSAGE );
                continue;
            }

            String normalized = choice.trim ().toLowerCase (Locale.ITALIAN);
            switch (normalized) {
                case "1", "setup" -> openConfiguratorSetupMenu();
                case "2", "list" -> openConfiguratorListMenu();
                case "3", "settings" -> openConfiguratorSettingsMenu();
                case BACK_COMMAND ->  stayInMenu = false;
                default -> printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
    }

    private boolean isBackCommand(String value) {
        return value != null && value.trim ().equalsIgnoreCase (BACK_COMMAND);
    }

    public boolean openConfiguratorSetupMenu(){
        boolean stayInMenu = true;
        while(stayInMenu){
            printer.println (MENU_SEPARATOR);
            printer.println ("Menu setup: ");
            printer.println ("1 - Aggiungi luogo");
            printer.println ("2 - Aggiungi visita");
            printer.println ("3 - aggiungi volontario");
            printer.println ("4 - Associa volontario a visita");
            printer.println ("back - Torna al menù principale");
            printer.println ("Menu setup: ");

            String choice = reader.readLine (DEFAULT_PROMPT).toLowerCase(Locale.ITALIAN);
            switch (choice){
                case "1" -> addPlace ();
                case "2" -> addVisitType();
                case "3" -> addVolunteer ();
                case "4" -> linkVolunteerToVisit();
                case BACK_COMMAND -> stayInMenu = false;
                default -> printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
        return true;
    }
    public boolean openConfiguratorListMenu(){
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println("Menu liste");
            printer.println("1 - Visualizza luoghi");
            printer.println("2 - Visualizza visite per luogo");
            printer.println("3 - Visualizza visite con stato");
            printer.println("4 - Visualizza volontari");
            printer.println("back - Torna al menu principale");
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine(DEFAULT_PROMPT).toLowerCase(Locale.ITALIAN);
            switch (choice) {
                case "1" -> listPlaces();
                case "2" -> listVisitTypeByPlace();
                case "3" -> listVisitTypeWithState();
                case "4" -> listVolunteer();
                case BACK_COMMAND -> stayInMenu = false;
                default -> printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
        return true;
    }
    public boolean openConfiguratorSettingsMenu(){
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println("Menu impostazioni");
            printer.println("1 - Imposta date precluse");
            printer.println("2 - Imposta max persone per iscrizione");
            printer.println("back - Torna al menu principale");
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

    private void runVolunteerSelfServiceMenu() {
        String nickname = requireActiveVolunteerNickname();
        boolean stayInMenu = true;
        while (stayInMenu) {
            printer.println(MENU_SEPARATOR);
            printer.println(VOLUNTEER_SELF_MENU_HEADER);
            printer.println(VOLUNTEER_SELF_MENU_SUBMIT_OPTION);
            printer.println(VOLUNTEER_SELF_MENU_AVAILABILITY_OPTION);
            printer.println(VOLUNTEER_SELF_MENU_SHIFTS_OPTION);
            printer.println(VOLUNTEER_SELF_MENU_BACK_OPTION);
            printer.println(MENU_SEPARATOR);

            String choice = reader.readLine(DEFAULT_PROMPT);
            if (choice == null) {
                printer.println(INVALID_COMMAND_MESSAGE);
                continue;
            }

            String normalized = choice.trim().toLowerCase(Locale.ITALIAN);
            switch (normalized) {
                case "1" -> submitVolunteerAvailability();
                case "2" -> listVolunteerAvailabilityForSelf();
                case "3" -> listshowVolunteerScheduleForSelf();
                case BACK_COMMAND, "logout" -> stayInMenu = false;
                default -> printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
    }

    sprivate boolean assignVolunteerShifts() {
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            YearMonth month = readYearMonth(VOLUNTEER_MONTH_PROMPT);
            List<AssignedShift> shifts = readAssignedShifts(month);
            volunteerService.assignShifts (nickname, month, shifts);
            printer.println (VOLUNTEER_SHIFT_SUCCESS);
        } catch(IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }

    private void listVolunteerSchedule() {
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            YearMonth month = readYearMonth(VOLUNTEER_MONTH_PROMPT);
            displayVolunteerSchedule(nickname, month);
        } catch(IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }

    private void listVolunteerAvailability() {
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            YearMonth month = readYearMonth(VOLUNTEER_MONTH_PROMPT);
            displayVolunteerAvailability(nickname, month);
        } catch(IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }

    private void displayVolunteerAvailability(String nickname, YearMonth month) {
        volunteerService.loadAvailability (nickname, month)
                .ifPresentOrElse (
                        availability -> printAvailability(nickname, availability),
                        () -> printer.println (VOLUNTEER_NO_AVAILABILITY.formatted (nickname, month))
                );
    }
    private void displayVolunteerSchedule(String nickname, YearMonth month) {
        List<AssignedShift> shifts = volunteerService.loadSchedule (nickname, month);
        if (shifts.isEmpty()) {
            printer.println (VOLUNTEER_NO_SHIFTS.formatted (nickname, month));
            return;
        }
        printer.println (VOLUNTEER_SCHEDULE_HEADER.formatted (nickname, month));
        shifts.stream ()
                .sorted ((left, right) -> {
                    int dateComparison = left.getDate ().compareTo (right.getDate ());
                    if (dateComparison != 0) {return dateComparison;}
                    return left.getVisitTypeId ().compareTo (right.getVisitTypeId ());
                })
                .map (this::formatShift)
                .forEach (printer::println);
    }
    private void submitVolunteerAvailability(){
        String nickname = requireActiveVolunteerNickname();
        YearMonth month = readYearMonthAllowingBack(VOLUNTEER_MONTH_PROMPT);
        if (month == null) {
            printer.println(OPERATION_ABORTED);
            return;
        }

        EnumSet<DayOfWeek> preferredDays = readPreferredDays();
        if (preferredDays == null) {
            printer.println(OPERATION_ABORTED);
            return;
        }

        Integer weeklyFrequency = readPositiveIntAllowingBack(VOLUNTEER_WEEKLY_FREQUENCY_PROMPT);
        if (weeklyFrequency == null) {
            printer.println(OPERATION_ABORTED);
            return;
        }

        LocalDate today = LocalDate.now();
        MonthlyAvailability availability = new MonthlyAvailability(month, preferredDays, weeklyFrequency, today);

        try {
            volunteerService.submitAvailability(nickname, availability, today);
            printer.println(VOLUNTEER_AVAILABILITY_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }
    private void listVolunteerAvailabilityForSelf() {
        String nickname = requireActiveVolunteerNickname();
        YearMonth month = readYearMonthAllowingBack(VOLUNTEER_MONTH_PROMPT);
        if (month == null) {
            return;
        }
        try {
            displayVolunteerAvailability(nickname, month);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }
    private void listVolunteerScheduleForSelf() {
        String nickname = requireActiveVolunteerNickname();
        YearMonth month = readYearMonthAllowingBack(VOLUNTEER_MONTH_PROMPT);
        if (month == null) {
            return;
        }
        try {
            displayVolunteerSchedule(nickname, month);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }

    private String requireActiveVolunteerNickname() {
        if (activeVolunteerNickname == null || activeVolunteerNickname.isBlank()) {
            throw new IllegalStateException("Nessun volontario autenticato.");
        }
        return activeVolunteerNickname;
    }

    private boolean authenticateVolunteer(String nickname) {
        while (true) {
            String password = reader.readLine(VOLUNTEER_LOGIN_PASSWORD_PROMPT);
            if (isBackCommand(password)) {
                printer.println(OPERATION_ABORTED);
                return false;
            }

            if (volunteerService.verifyLogin(nickname, password)) {
                printer.println(VOLUNTEER_LOGIN_SUCCESS);
                return true;
            }

            printer.println(VOLUNTEER_LOGIN_FAILURE);
        }
    }


    public boolean listPlaces(){
        printer.println (Printer.PLACE_HEADER);
        List<PlaceDTO> places = service.listPlace ();
        printer.printPlaceList (places);
        return true;
    }
    public boolean listVisitTypeByPlace(){
        List<PlaceDTO> places = service.listPlace ();
        printer.println (Printer.PLACE_HEADER);
        printer.printPlaceList (places);
        if (places == null || places.isEmpty ()) return true;
        String placeId= reader.readLine (PLACE_SELECTION_PROMPT);
        try{
            List<VisitTypeDTO> visitTypes = service.listVisitTypeByPlace (placeId);
            printer.println (Printer.VISIT_TYPE_HEADER);
            printer.printVisitTypeList (visitTypes);
        }catch (IllegalArgumentException ex){
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
        return true;
    }

    public boolean listVisitTypeWithState(){
        try{
            List<VisitTypeDTO> visitTypes = service.listVisitType ();
            printer.println (Printer.VISIT_TYPE_HEADER);
            printer.printVisitTypeList (visitTypes);
        }catch (IllegalArgumentException ex){
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
        return true;
    }

    public boolean listVolunteer(){
        printer.println (Printer.VOLUNTEER_HEADER);
        List<VolunteerDTO> volunteers = service.listVolunteerWVisitType ();
        printer.printVolunteerList (volunteers);
        return true;
    }

    public boolean setBlackOutDates(){
        List<String> rawDates = reader.readValues(buildExcludedDatesPrompt(), null);
        if(rawDates.size () == 1){
            String token = rawDates.get(0).toLowerCase (Locale.ITALIAN);
            if(CLEAR_TOKEN.equals(token) || CLEAR_TOKEN_ALT.equals(token)){
                rawDates = List.of();
            }
        }

        if(rawDates.isEmpty()) {
            try{
                service.setBlackoutDates (List.of());
                printer.println (PRECLUDED_DATES_SUCCESS);
            }catch(IllegalArgumentException | IllegalStateException ex) {
                printer.println(ERROR_PREFIX + safeMessage(ex));
            }
            return true;
        };



        List<LocalDate> dates = new ArrayList<> ();
        for (String rawDate : rawDates){
            try{
                dates.add(LocalDate.parse(rawDate, DATE_FORMATTER));
            }catch(DateTimeParseException ex){
                printer.println(ERROR_PREFIX + PRECLUDED_DATES_INVALID_FORMAT.formatted (rawDate));
                return true;
            }
        }

        try{
            service.setBlackoutDates (dates);
            printer.println (PRECLUDED_DATES_SUCCESS);
        }catch(IllegalArgumentException | IllegalStateException ex){
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
        return true;
    }

    private String buildExcludedDatesPrompt(){
        YearMonth allowedMonth = ExcludedDatePolicy.allowedMonth(LocalDate.now());
        String monthLabel = allowedMonth.format(YEAR_MONTH_FORMATTER);
        return "Inserisci le date precluse per " + monthLabel + " separate da virgola (formato gg-mm-yyyy). Digita '-'/'nessuna' per non impostarne: ";
    }

    public boolean setMaxPeoplePerSub(){
       String value = reader.readLine(MAX_PEOPLE_PROMPT);
       int max;
       try{
           max = Integer.parseInt(value);
       }catch(NumberFormatException ex){
           printer.println (ERROR_PREFIX + INVALID_NUMBER_MESSAGE);
           return true;
       }

       try{
           service.setMaxPeoplePerSubscription (max);
           printer.println (MAX_PEOPLE_SUCCESS);
       }catch(IllegalArgumentException | IllegalStateException ex){
           printer.println (ERROR_PREFIX + safeMessage(ex));
       }
       return true;
    }

    private String safeMessage(RuntimeException ex){
        String message = ex.getMessage();
        return message == null ? "" : message;
    }

    private void addPlace(){
        try {
            String title = Objects.requireNonNull (reader.readLine (PLACE_NAME_PROMPT));
            String description = Objects.requireNonNull (reader.readLine (PLACE_DESCRIPTION_PROMPT));
            String location = Objects.requireNonNull (reader.readLine (PLACE_LOCATION_PROMPT));
            String result = service.addPlace (title, description, location);
            printer.println (result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }
    private void addVisitType(){
        try {
            String placeId = Objects.requireNonNull (reader.readLine (VISIT_PLACE_PROMPT));
            String title = Objects.requireNonNull (reader.readLine (VISIT_TITLE_PROMPT));
            String description = Objects.requireNonNull (reader.readLine (VISIT_DESCRIPTION_PROMPT));
            String meetingLocation = Objects.requireNonNull (reader.readLine (VISIT_MEETING_PROMPT));
            List<TimeSlot> schedules = readVisitSchedules ();
            boolean ticketRequired = reader.readBoolean (VISIT_TICKET_PROMPT);
            int minParticipants = readPositiveInt (VISIT_MIN_PROMPT);
            int maxParticipants = readMaxParticipants (minParticipants);
            LocalDate validFrom = readDate (VISIT_VALID_FROM_PROMPT);
            LocalDate validTo = readDate (VISIT_VALID_TO_PROMPT);
            if (validTo.isBefore (validFrom)) {
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
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }
    private List<TimeSlot> readVisitSchedules(){
        List<TimeSlot> schedules = new ArrayList<>();
        printer.println ("Configura gli orari della visita");
        boolean addMore;
        do{
            DayOfWeek day = readDayOfWeek();
            LocalTime startTime = readTime(VISIT_START_PROMPT);
            int durationMinutes = readPositiveInt(VISIT_DURATION_PROMPT);
            schedules.add (new TimeSlot (day, startTime, Duration.ofMinutes (durationMinutes)));
            addMore = reader.readBoolean (VISIT_ANOTHER_SLOT_PROMPT);
        } while (addMore);
        return schedules;
    }

    private DayOfWeek readDayOfWeek(){
        while(true){
            String value = reader.readLine (VISIT_DAY_PROMPT);
            try{
                int numeric = Integer.parseInt(value);
                if(numeric >= 1 && numeric <= 7){
                    return DayOfWeek.of(numeric);
                }
            } catch(NumberFormatException ignored){}

            String normalized = value.trim().toLowerCase (Locale.ITALIAN).replace ('ì', 'i');
            switch (normalized){
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
                default -> {
                    printer.println(ERROR_PREFIX + "Giorno non valido. Usa un numero tra 1 e 7 o il nome del giorno.");
                }
            }
        }
    }
    private LocalTime readTime(String prompt) {
        while (true){
            String value = reader.readLine (prompt);
            try{
                return LocalTime.parse(value, TIME_FORMATTER);
            } catch(DateTimeParseException ex){
                printer.println(ERROR_PREFIX + "Fromato orario non valido. Usa HH:mm");
            }
        }
    }
    private int readPositiveInt(String prompt) {
        while (true){
            String value = reader.readLine (prompt);
            try{
                int parsed = Integer.parseInt (value);
                if (parsed > 0) return parsed;
            } catch(NumberFormatException ex){}

            printer.println(ERROR_PREFIX + INVALID_NUMBER_MESSAGE);
        }
    }
    private int readMaxParticipants(int minParticipants){
        while (true){
            int maxParticipants = readPositiveInt (VISIT_MAX_PROMPT);
            if (maxParticipants >= minParticipants) return maxParticipants;
            printer.println(ERROR_PREFIX + "Il numero massimo di iscrizioni deve essere maggiore o uguale al minimo indicato");
        }
    }
    private LocalDate readDate (String prompt) {
        while (true){
            String value = reader.readLine (prompt);
            try{
                return LocalDate.parse(value, DATE_FORMATTER);
            } catch(DateTimeParseException ex){
                printer.println (ERROR_PREFIX + PRECLUDED_DATES_INVALID_FORMAT.formatted (value));
            }
        }
    }

    private void addVolunteer(){
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            String password = Objects.requireNonNull (reader.readLine (VOLUNTEER_PASSWORD_PROMPT));
            service.addVolunteer (nickname, password);
            printer.println (VOLUNTEER_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void linkVolunteerToVisit(){
        try {
            String nickname = Objects.requireNonNull (reader.readLine (VOLUNTEER_NICKNAME_PROMPT));
            String visitTitle = Objects.requireNonNull (reader.readLine (VOLUNTEER_LINK_VISIT_PROMPT));
            service.linkVOlunteerToVisit (nickname, visitTitle);
            printer.println (VOLUNTEER_LINK_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

}
