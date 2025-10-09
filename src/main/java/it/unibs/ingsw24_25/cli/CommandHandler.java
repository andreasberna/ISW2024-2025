package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandHandler {
    private static final String PLACE_SELECTION_PROMPT = "Inserisci il titolo del luogo di cui visualizzare le visite: ";
    private static final String ERROR_PREFIX = "Errore: ";
    private static final String PRECLUDED_DATES_PROMPT = "Inserisci le date precluse separate da virgola (formato yyyy-MM-dd): ";
    private static final String MAX_PEOPLE_PROMPT = "Inserisci il numero massimo di persone per iscrizione: ";
    private static final String INVALID_NUMBER_MESSAGE = "Inserire un numero intero valido.";
    private static final String PRECLUDED_DATES_SUCCESS = "Date precluse aggiornate.";
    private static final String MAX_PEOPLE_SUCCESS = "Numero massimo di persone per iscrizione aggiornato.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM yyyy", Locale.ITALIAN);

    private final ConfiguratorService service;
    private final Printer printer;
    private final PromptReader reader;

    public CommandHandler(ConfiguratorService service, Printer printer, PromptReader prompt) {
        this.service = service;
        this.printer = printer;
        this.reader = prompt;
    }

    public boolean handle(String command){
        if (command == null) return false;

        String normalized = command.trim();
        if(normalized.isEmpty()) return false;

        return switch (normalized){
            case "1" -> listPlaces();
            case "2" -> listVisitTypeByPlace();
            case "3" -> listVisitTypeWithState();
            case "4" -> listVolunteer();
            case "5" -> setBlackOutDates();
            case "6" -> setMaxPeoplePerSub();
            default  -> false;
        };
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
        List<String> rawDates = reader.readValues(PRECLUDED_DATES_PROMPT, null);
        if(rawDates.size () == 1){
            String token = rawDates.get(0);
            if("-".equals(token) || "nessuna".equals(token)){
                rawDates = List.of();
            }
        }

        if(rawDates.isEmpty()) {
            try{
                service.setBlackoutDates (List.of());
                printer.println (PRECLUDED_DATES_SUCCESS);
            }catch(IllegalArgumentException | IllegalStateException ex) {
                printer.println(ERROR_PREFIX + "Formato non valido: " + rawDates + ". Usa dd-mm-yyyy");
                return true;
            }
        };

        List<LocalDate> dates = new ArrayList<> ();
        for (String rawDate : rawDates){
            try{
                dates.add(LocalDate.parse(rawDate, DATE_FORMATTER));
            }catch(DateTimeParseException ex){
                printer.println(ERROR_PREFIX + "Formato data non valido: " + rawDate + ". Usa gg-mm-yyyy");
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
}
