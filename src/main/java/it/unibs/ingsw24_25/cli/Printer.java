package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.VisitState;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Printer {
    public static final String PLACE_HEADER = "Luoghi disponibili:";
    public static final String PLACE_EMPTY_MESSAGE = "Nessun luogo registrato al momento";
    public static final String VISIT_TYPE_HEADER = "Visite disponibili:";
    public static final String VISIT_TYPE_EMPTY_MESSAGE = "Nessuna visita registrata al momento";
    public static final String VOLUNTEER_HEADER = "Volontari disponibili:";
    public static final String VOLUNTEER_EMPTY_MESSAGE = "Nessun volontario disponibile al momento";

    private final PrintStream out;

    public Printer(PrintStream out) {
        this.out = Objects.requireNonNull(out, "PrintStream non può essere nullo");
    }

    public void println(String text) {
        out.println(text);
    }

    public void print(String text) {
        out.print(text);
    }

    public void printPlaceList(List<PlaceDTO> places) {
        if (places == null || places.isEmpty()) {
            out.println(PLACE_EMPTY_MESSAGE);
            return;
        }

        places.stream()
                .map(this::format)
                .forEach(out::println);
    }

    public void printVisitTypeList(List<VisitTypeDTO> visitTypes) {
        if (visitTypes == null || visitTypes.isEmpty()) {
            out.println(VISIT_TYPE_EMPTY_MESSAGE);
            return;
        }

        visitTypes.stream()
                .map(this::format)
                .forEach(out::println);
    }

    public void printVolunteerList(List<VolunteerDTO> volunteers) {
        if (volunteers == null || volunteers.isEmpty()) {
            out.println(VOLUNTEER_EMPTY_MESSAGE);
            return;
        }

        volunteers.stream()
                .map(this::format)
                .forEach(out::println);
    }

    private String format(PlaceDTO place) {
        if (place == null) {
            return "";
        }

        return ("Luogo: %s%nDescrizione: %s%nUbicazione: %s" + System.lineSeparator())
                .formatted(
                        safe(place.getTitle()),
                        safe(place.getDescription()),
                        safe(place.getLocation())
                );
    }

    private String format(VisitTypeDTO visitType) {
        if (visitType == null) {
            return "";
        }

        return ("Visita: %s%nGiorni: %s%nOrario: %s - %s%nDurata: %d minuti%nTicket richiesto: %s%nPartecipanti: %d-%d%nStato: %s" + System.lineSeparator())
                .formatted(
                        safe(visitType.getTitle()),
                        safe(visitType.getDaySummary()),
                        safe(visitType.getStartTime()),
                        safe(visitType.getendTime()),
                        visitType.getDurationMinutes(),
                        visitType.isTicketRequired() ? "Sì" : "No",
                        visitType.getMinParticipants(),
                        visitType.getMaxParticipants(),
                        formatState(visitType.getState ())
                );
    }

    private String format(VolunteerDTO volunteer) {
        if (volunteer == null) {
            return "";
        }

        String visits = volunteer.getVisitTypeTitles() == null || volunteer.getVisitTypeTitles().isEmpty()
                ? "Nessuna visita assegnata"
                : volunteer.getVisitTypeTitles().stream()
                .map(this::safe)
                .collect(Collectors.joining(", "));

        return ("Volontario: %s%nVisite: %s" + System.lineSeparator())
                .formatted(safe(volunteer.getNickname()), visits);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatState(VisitState visit){
        if (visit == null) { return "non specificato";}

        return switch(visit) {
            case PROPOSTA -> "Proposta";
            case COMPLETA -> "Completa";
            case CONFERMATA -> "Confermata";
            case CANCELLATA -> "Cancellata";
            case EFFETTUATA ->  "Effettata";
        };
    }
}
