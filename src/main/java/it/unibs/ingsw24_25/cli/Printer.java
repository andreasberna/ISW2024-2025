package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;

import java.io.PrintStream;
import java.util.List;

public class Printer {
    private static final String DEFAULT_PLACE_HEADER = "Luoghi disponibili:";
    private static final String DEFAULT_VISITTYPE_HEADER = "Tipi di visita:";
    private static final String DEFAULT_VOLUNTEER_HEADER = "Volontari registrati:";
    private static final String DEFAULT_EMPTY_PLACE = "Nessun luogo disponibile.";
    private static final String DEFAULT_EMPTY_VISITTYPE = "Nessun tipo di visita disponibile.";
    private static final String DEFAULT_EMPTY_VOLUNTEER = "Nessun voluntari disponibile.";

    private PrintStream out;

    public Printer(PrintStream out) {
        this.out = out;
    }

    public void println(String text){
        out.println(text);
    }
    public void printError(String message){
        out.println("[ERRORE] " + message);
    }
    public void printPlaceList(List<PlaceDTO> list){
        out.println(DEFAULT_PLACE_HEADER);
        if(list == null || list.isEmpty()){
            out.println(" " +  DEFAULT_EMPTY_PLACE);
            return;
        }
        list.forEach(p->out.println(" -" + format(p)));
    }
    public void printVisitTypeList(List<VisitTypeDTO> list){
        out.println(DEFAULT_VISITTYPE_HEADER);
        if(list == null || list.isEmpty()){
            out.println(" " +  DEFAULT_EMPTY_VISITTYPE);
            return;
        }

        list.forEach (vt -> {
            out.println (" -" + format(vt));
        });
    }
    public void printVolunteerList(List<VolunteerDTO> list){
        out.println(DEFAULT_VOLUNTEER_HEADER);
        if(list == null || list.isEmpty()){
            out.println(" " +  DEFAULT_EMPTY_VOLUNTEER);
            return;
        }
        list.forEach (v -> {
            out.println (" -" + v.getNickname ());
            v.getVisitTypeTitles ().forEach (vvt -> out.println ("  -" + vvt));
        });
    }

    public void printVisitTypeState(List<VisitTypeDTO> list){
        out.println(DEFAULT_VISITTYPE_HEADER);
        if(list == null || list.isEmpty()){
            println(" " +  DEFAULT_EMPTY_VISITTYPE);
            return;
        }
        list.forEach (vt -> {
            out.println(" -" + vt.getTitle () + ": " + vt.getState ().toString ());
        });

    }
    public void printHelp(){
        out.println("Comandi disponibili:");
        out.println("  help                                                 - mostra questo messaggio");
        out.println("  aggiungi luogo                                       - aggiungi un luogo alla lista di luoghi disponibili");
        out.println("  aggiungi tipo di visita                              - aggiungi un nuovo tipo di visita ad un luogo");
        out.println("  modifica numero massimo di persone per iscrizione    ");
        out.println("  inserisci date da precludere                         - inserisci le date da precludere alle visite");
        out.println("  list places                                          - elenca tutti i luoghi");
        out.println("  list visit-types per luogo                           - elenca le tipologie di visita con il luogo associato");
        out.println("  list visit-types per stato                           - elenca le tipologie di visita con lo stato associato");
        out.println("  list volunteers                                      - elenca i volontari registrati");
        out.println("  exit                                                 - termina l'applicazione");
    }

    private String format(Object dto){
        return dto == null ? "<n.d.>" : dto.toString();
    }
}
