package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.service.BeneficiaryService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BeneficiaryCommandHandler {

    private static final String MENU_SEPARATOR = "-----------------------";
    private static final String INVALID_COMMAND_MESSAGE = "Comando non valido";
    private static final String ERROR_PREFIX = "Errore: ";
    private static final String OPERATION_ABORTED = "Operazione annullata.";
    private static final String DEFAULT_PROMPT = "> ";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BeneficiaryService beneficiaryService;
    private final Printer printer;
    private final PromptReader reader;

    private String activeUsername;

    public BeneficiaryCommandHandler(BeneficiaryService beneficiaryService, Printer printer, PromptReader reader) {
        this.beneficiaryService = Objects.requireNonNull(beneficiaryService);
        this.printer = Objects.requireNonNull(printer);
        this.reader = Objects.requireNonNull(reader);
    }

    public void startSession(){
        boolean exit = false;
        while (!exit) {
            printer.println (MENU_SEPARATOR);
            printer.println ("Accesso fruitore");
            printer.println ("1- Registrati");
            printer.println ("2- Accedi");
            printer.println ("back - Torna Indietro");
            printer.println (MENU_SEPARATOR);

            String choice = reader.readLine (DEFAULT_PROMPT);
            if (choice == null){
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }
            String normalized = choice.trim ().toLowerCase (Locale.ITALIAN);
            switch (normalized) {
                case "1", "registrati" : register();
                case "2", "accedi", "login" : {
                    if (authenticate()){
                        runBeneficiaryMenu();
                        activeUsername = null;
                        exit = true;
                    }
                }
                case "back" : exit = true;
                default : printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
    }

    private void register() {
        String fullName = reader.readLine ("Nome e Cognome: ");
        if (isBack(fullName)){
            printer.println (OPERATION_ABORTED);
            return;
        }
        String username = reader.readLine ("Scegli uno username: ");
        if (isBack(username)){
            printer.println (OPERATION_ABORTED);
            return;
        }
        String password = reader.readLine ("Scegli una password: ");
        if (isBack(password)){
            printer.println (OPERATION_ABORTED);
            return;
        }
        try{
            beneficiaryService.register (fullName, username, password);
            printer.println ("Registrazione completata. Ora puoi accedere.");
        } catch (IllegalArgumentException | IllegalStateException ex){
            printer.println (ERROR_PREFIX + safeMessage(ex));
        }
    }

    private boolean authenticate(){
        while (true){
            String username = reader.readLine ("Username: ");
            if (isBack(username)) {
                printer.println(OPERATION_ABORTED);
                return false;
            }
            String password = reader.readLine("Password: ");
            if (isBack(password)) {
                printer.println(OPERATION_ABORTED);
                return false;
            }
            if (beneficiaryService.verifyLogin (username,password)){
                this.activeUsername = username.trim ();
                printer.println ("Accesso effettuato.");
                return true;
            }
            printer.println ("Credenziali non valide");
        }
    }

    private void runBeneficiaryMenu() {
        boolean stay = true;
        while (stay) {
            printer.println (MENU_SEPARATOR);
            printer.println ("Menù fruitore");
            printer.print ("1 - Visualizza visite");
            printer.print ("2 - Gestisci prenotazioni");
            printer.print ("logout");
            printer.println (MENU_SEPARATOR);
            String choice = reader.readLine (DEFAULT_PROMPT);
            if (choice == null){
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }
            String normalized = choice.trim ().toLowerCase (Locale.ITALIAN);
            switch (normalized) {
                case "1": runBeneficiaryVisitsMenu();
                case "2": runBeneficiaryBookingMenu();
                case "logout" : stay = false;
                default : printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
    }

    private void runBeneficiaryVisitsMenu() {
        boolean stay = true;
        while (stay) {
            printer.println (MENU_SEPARATOR);
            printer.println ("Menu Visite");
            printer.print ("1 - Visualizza visite proposte");
            printer.print ("2 - Visualizza visite confermate");
            printer.print ("back - Torna Indietro");
            printer.println (MENU_SEPARATOR);
            String choice = reader.readLine (DEFAULT_PROMPT);
            if (choice == null){
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }
            String normalized = choice.trim ().toLowerCase (Locale.ITALIAN);
            switch (normalized) {
                case "1", "proposte" : displayVisits(VisitStatus.PROPOSED);
                case "2", "confermate" : displayVisits(VisitStatus.CONFIRMED);
                case "back" : stay = false;
                default : printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
    }

    private void runBeneficiaryBookingMenu() {
        boolean stay = true;
        while (stay) {
            printer.println (MENU_SEPARATOR);
            printer.println ("Menu Prenotazioni");
            printer.print ("1 - Prenota visita proposta");
            printer.print ("2 - Le mie prenotazioni");
            printer.print ("3 - Annulla prenotazione");
            printer.print ("back - Torna Indietro");
            printer.println (MENU_SEPARATOR);
            String choice = reader.readLine (DEFAULT_PROMPT);
            if (choice == null){
                printer.println (INVALID_COMMAND_MESSAGE);
                continue;
            }
            String normalized = choice.trim ().toLowerCase (Locale.ITALIAN);
            switch (normalized) {
                case "1", "prenota" : bookVisitFlow();
                case "2" : displayBookings();
                case "3", "cancel" : cancelBooking();
                case "back" : stay = false;
                default : printer.println (INVALID_COMMAND_MESSAGE);
            }
        }
    }

    private void displayVisits(VisitStatus status) {
        try {
            List<VisitOccurrenceDTO> visits = beneficiaryService.listVisitsByStatus(status);
            if (visits.isEmpty()) {
                printer.println ("Nessuna visita " + status.name ().toLowerCase (Locale.ITALIAN) + " disponibile.");
                return;
            }
            visits.stream ()
                    .sorted (Comparator.comparing (VisitOccurrenceDTO::getDate))
                    .map (this::formatVisit)
                    .forEach (printer::println);
        } catch (RuntimeException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }

    private void bookVisitFlow() {
        String username = requireActiveUsername ();
        List<VisitOccurrenceDTO> visits = beneficiaryService.listVisitsByStatus (VisitStatus.PROPOSED);
        if (visits.isEmpty()) {
            printer.println ("Nessuna visita prenotabile al momento.");
            return;
        }
        printer.println ("Visite prenotabili:");
        visits.stream ().map (this::formatVisit).forEach (printer::println);
        String visitId = reader.readLine ("Inserisci l'ID della visita: ");
        if (isBack (visitId)) {
            printer.println (OPERATION_ABORTED);
            return;
        }
        Integer participants = readPositiveInt ("Numero di partecipanti: ");
        if (participants == null) {
            printer.println (OPERATION_ABORTED);
            return;
        }
        String notes = reader.readLine ("Note(facoltative, \"back\" per annullare): ");
        if (isBack (notes)) {
            printer.println (OPERATION_ABORTED);
            return;
        }

        try {
            String code = beneficiaryService.bookVisit (username, visitId, participants, notes, LocalDate.now());
            printer.println ("Prenotazione effettuata. Codice: " + code);
        } catch (RuntimeException ex) {
            printer.println (ERROR_PREFIX + safeMessage (ex));
        }
    }

    private void displayBookings() {
        try {
            List<VisitBookingDTO> bookings = beneficiaryService.listBookings(requireActiveUsername());
            if (bookings.isEmpty()) {
                printer.println("Nessuna prenotazione effettuata.");
                return;
            }
            bookings.stream()
                    .sorted(Comparator.comparing(VisitBookingDTO::getVisitDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::formatBooking)
                    .forEach(printer::println);
        } catch (RuntimeException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }

    private void cancelBooking() {
        String code = reader.readLine("Inserisci il codice prenotazione: ");
        if (isBack(code)) {
            printer.println(OPERATION_ABORTED);
            return;
        }
        try {
            beneficiaryService.cancelBooking(requireActiveUsername(), code, LocalDate.now());
            printer.println("Prenotazione annullata.");
        } catch (RuntimeException ex) {
            printer.println(ERROR_PREFIX + safeMessage(ex));
        }
    }

    private Integer readPositiveInt(String prompt) {
        while (true) {
            String value = reader.readLine(prompt);
            if (isBack(value)) {
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

    private String formatVisit(VisitOccurrenceDTO visit) {
        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(visit.getId()).append(System.lineSeparator());
        builder.append("Titolo: ").append(visit.getTitle()).append(System.lineSeparator());
        builder.append("Descrizione: ").append(visit.getDescription()).append(System.lineSeparator());
        builder.append("Punto di ritrovo: ").append(visit.getMeetingPoint()).append(System.lineSeparator());
        builder.append("Data: ").append(formatDate(visit.getDate())).append(System.lineSeparator());
        builder.append("Ora: ").append(visit.getStartTime() == null ? "-" : visit.getStartTime()).append(System.lineSeparator());
        builder.append("Biglietto richiesto: ").append(visit.isTicketRequired() ? "Sì" : "No").append(System.lineSeparator());
        builder.append("Prenotati: ").append(visit.getBookedParticipants()).append(" / ").append(visit.getMaxParticipants());
        return builder.toString();
    }

    private String formatBooking(VisitBookingDTO booking) {
        StringBuilder builder = new StringBuilder();
        builder.append("Codice: ").append(booking.getCode()).append(System.lineSeparator());
        builder.append("Visita: ").append(booking.getVisitTitle()).append(System.lineSeparator());
        builder.append("Data: ").append(formatDate(booking.getVisitDate())).append(System.lineSeparator());
        builder.append("Partecipanti: ").append(booking.getParticipants()).append(System.lineSeparator());
        if (booking.getNotes() != null && !booking.getNotes().isBlank()) {
            builder.append("Note: ").append(booking.getNotes()).append(System.lineSeparator());
        }
        builder.append("Stato visita: ").append(booking.getStatus() == null ? "-" : booking.getStatus().name());
        return builder.toString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private boolean isBack(String value) {
        return value != null && value.trim().equalsIgnoreCase("back");
    }

    private String requireActiveUsername() {
        if (activeUsername == null || activeUsername.isBlank()) {
            throw new IllegalStateException("Nessun fruitore autenticato");
        }
        return activeUsername;
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? "" : message;
    }
}
