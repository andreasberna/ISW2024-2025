package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.service.VolunteerService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;

public class VolunteerCommandHandler {
    private static final String MENU_SEPARATOR = "-----------------------";
    private static final String DEFAULT_PROMPT = "> ";
    private static final String BACK_COMMAND = "back";
    private static final String INVALID_COMMAND_MESSAGE = "Comando non valido";
    private static final String ERROR_PREFIX = "Errore: ";
    private static final String OPERATION_ABORTED = "Operazione annullata.";
    private static final String MONTH_INPUT_INVALID = "Formato mese non valido. Usa yyyy-MM.";
    private static final String VOLUNTEER_LOGIN_PROMPT = "Nickname volontario (\"back\" per annullare): ";
    private static final String VOLUNTEER_LOGIN_PASSWORD_PROMPT = "Password: ";
    private static final String VOLUNTEER_LOGIN_FAILURE = "Credenziali non valide. Riprova.";
    private static final String VOLUNTEER_LOGIN_SUCCESS = "Accesso effettuato.";
    private static final String VOLUNTEER_SELF_MENU_HEADER = "Menu volontario";
    private static final String VOLUNTEER_SELF_MENU_SUBMIT_OPTION = "1 - Invia disponibilità mensile";
    private static final String VOLUNTEER_SELF_MENU_AVAILABILITY_OPTION = "2 - Visualizza disponibilità mensile";
    private static final String VOLUNTEER_SELF_MENU_SHIFTS_OPTION = "3 - Visualizza turni assegnati";
    private static final String VOLUNTEER_SELF_MENU_BACK_OPTION = "back - Esci";
    private static final String VOLUNTEER_MONTH_PROMPT = "Inserisci il mese di riferimento (yyyy-MM): ";
    private static final String VOLUNTEER_WEEKLY_FREQUENCY_PROMPT = "Quante disponibilità settimanali puoi garantire? ";
    private static final String VOLUNTEER_PREFERRED_DAYS_PROMPT = "Giorni disponibili (es. 1,3,5 o lun,mer,ven). Digita \"back\" per annullare: ";
    private static final String VOLUNTEER_INVALID_DAY_INPUT = "Inserire almeno un giorno valido della settimana.";
    private static final String VOLUNTEER_UNKNOWN_DAY = "Giorno non riconosciuto: %s.";
    private static final String VOLUNTEER_AVAILABILITY_SUCCESS = "Disponibilità registrata correttamente.";
    private static final DateTimeFormatter YEAR_MONTH_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final VolunteerService volunteerService;
    private final FirstAccessSetup firstAccessSetup;
    private final Printer printer;
    private final PromptReader reader;
    private final CommandRouter volunteerMenuRouter;

    private String activeVolunteerNickname;

    public VolunteerCommandHandler(VolunteerService volunteerService,
                                   FirstAccessSetup firstAccessSetup,
                                   Printer printer,
                                   PromptReader reader) {
        this.volunteerService = Objects.requireNonNull(volunteerService);
        this.firstAccessSetup = Objects.requireNonNull(firstAccessSetup);
        this.printer = Objects.requireNonNull(printer);
        this.reader = Objects.requireNonNull(reader);
        this.volunteerMenuRouter = configureVolunteerRouter();
    }

    private CommandRouter configureVolunteerRouter() {
        CommandRouter router = new CommandRouter();
        router.register("1", this::submitVolunteerAvailability);
        router.register("disponibilita", this::submitVolunteerAvailability);
        router.register("2", this::showVolunteerAvailability);
        router.register("availability", this::showVolunteerAvailability);
        router.register("3", this::showVolunteerSchedule);
        router.register("turni", this::showVolunteerSchedule);
        router.register("shifts", this::showVolunteerSchedule);
        return router;
    }

    public void startSession() {
        while (true) {
            printer.println(MENU_SEPARATOR);
            printer.println("Accesso volontario");
            printer.println(MENU_SEPARATOR);

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
                    this.activeVolunteerNickname = activeNickname;
                    runVolunteerSelfServiceMenu();
                    this.activeVolunteerNickname = null;
                    return;
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                printer.println(ERROR_PREFIX + safeMessage(ex));
            }
        }
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

    private void runVolunteerSelfServiceMenu() {
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
            if (normalized.isEmpty()) {
                printer.println(INVALID_COMMAND_MESSAGE);
                continue;
            }

            if (BACK_COMMAND.equals(normalized) || "logout".equals(normalized)) {
                stayInMenu = false;
                continue;
            }

            if (!volunteerMenuRouter.route(normalized)) {
                printer.println(INVALID_COMMAND_MESSAGE);
            }
        }
    }

    private boolean submitVolunteerAvailability() {
        String nickname = requireActiveVolunteerNickname();
        YearMonth month = readYearMonthAllowingBack(VOLUNTEER_MONTH_PROMPT);
        if (month == null) {
            printer.println(OPERATION_ABORTED);
            return true;
        }

        EnumSet<DayOfWeek> preferredDays = readPreferredDays();
        if (preferredDays == null) {
            printer.println(OPERATION_ABORTED);
            return true;
        }

        Integer weeklyFrequency = readPositiveIntAllowingBack(VOLUNTEER_WEEKLY_FREQUENCY_PROMPT);
        if (weeklyFrequency == null) {
            printer.println(OPERATION_ABORTED);
            return true;
        }

        LocalDate today = LocalDate.now();
        MonthlyAvailability availability = new MonthlyAvailability(month, preferredDays, weeklyFrequency, today);

        try {
            volunteerService.submitAvailability(nickname, availability, today);
            printer.println(VOLUNTEER_AVAILABILITY_SUCCESS);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
        return true;
    }

    private boolean showVolunteerAvailability() {
        String nickname = requireActiveVolunteerNickname();
        YearMonth month = readYearMonthAllowingBack(VOLUNTEER_MONTH_PROMPT);
        if (month == null) {
            return true;
        }

        try {
            VolunteerCliSupport.displayAvailability(volunteerService, printer, nickname, month, YEAR_MONTH_INPUT_FORMATTER);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
        return true;
    }

    private boolean showVolunteerSchedule() {
        String nickname = requireActiveVolunteerNickname();
        YearMonth month = readYearMonthAllowingBack(VOLUNTEER_MONTH_PROMPT);
        if (month == null) {
            return true;
        }

        try {
            VolunteerCliSupport.displaySchedule(volunteerService, printer, nickname, month, YEAR_MONTH_INPUT_FORMATTER);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
        return true;
    }

    private String requireActiveVolunteerNickname() {
        if (activeVolunteerNickname == null || activeVolunteerNickname.isBlank()) {
            throw new IllegalStateException("Nessun volontario autenticato.");
        }
        return activeVolunteerNickname;
    }

    private YearMonth readYearMonthAllowingBack(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            if (isBackCommand(value)) {
                return null;
            }
            try {
                return YearMonth.parse(value, YEAR_MONTH_INPUT_FORMATTER);
            } catch (DateTimeParseException ex) {
                printer.println(ERROR_PREFIX + MONTH_INPUT_INVALID);
            }
        }
    }

    private EnumSet<DayOfWeek> readPreferredDays() {
        while (true) {
            String raw = reader.readLine(VOLUNTEER_PREFERRED_DAYS_PROMPT);
            if (isBackCommand(raw)) {
                return null;
            }
            if (raw == null || raw.isBlank()) {
                printer.println(ERROR_PREFIX + VOLUNTEER_INVALID_DAY_INPUT);
                continue;
            }

            String[] tokens = raw.split(",");
            EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            boolean invalid = false;
            for (String token : tokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }
                try {
                    days.add(parseDayToken(token));
                } catch (IllegalArgumentException ex) {
                    printer.println(ERROR_PREFIX + VOLUNTEER_UNKNOWN_DAY.formatted(token.trim()));
                    invalid = true;
                    break;
                }
            }

            if (invalid) {
                continue;
            }

            if (days.isEmpty()) {
                printer.println(ERROR_PREFIX + VOLUNTEER_INVALID_DAY_INPUT);
                continue;
            }

            return days;
        }
    }

    private DayOfWeek parseDayToken(String raw) {
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }

        try {
            int numeric = Integer.parseInt(normalized);
            if (numeric >= 1 && numeric <= 7) {
                return DayOfWeek.of(numeric);
            }
        } catch (NumberFormatException ignored) {
        }

        normalized = normalized.toLowerCase(Locale.ITALIAN).replace('ì', 'i');
        return switch (normalized) {
            case "lunedi", "lun", "monday" -> DayOfWeek.MONDAY;
            case "martedi", "mar", "tuesday" -> DayOfWeek.TUESDAY;
            case "mercoledi", "mer", "wednesday" -> DayOfWeek.WEDNESDAY;
            case "giovedi", "gio", "thursday" -> DayOfWeek.THURSDAY;
            case "venerdi", "ven", "friday" -> DayOfWeek.FRIDAY;
            case "sabato", "sab", "saturday" -> DayOfWeek.SATURDAY;
            case "domenica", "dom", "sunday" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("invalid");
        };
    }

    private Integer readPositiveIntAllowingBack(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            if (isBackCommand(value)) {
                return null;
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }

            printer.println(ERROR_PREFIX + "Inserire un numero intero valido.");
        }
    }

    private boolean isBackCommand(String value) {
        return value != null && value.trim().equalsIgnoreCase(BACK_COMMAND);
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? "" : message;
    }

}
