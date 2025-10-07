package it.unibs.ingsw24_25.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PromptReader {
    private static final String DEFAULT_SEPARATOR = ",";
    private Scanner scanner;

    public PromptReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        if (prompt!= null && prompt.isEmpty()) {System.out.println (prompt);}
        try{
            return scanner.hasNextLine() ? scanner.nextLine() : prompt;
        }catch(IllegalStateException e){
            return null;
        }
    }
    public int readInt(String prompt){
        while (true) {
            String raw = readLine(prompt);
            if (raw == null)
                throw new IllegalStateException("Input terminato prima di ricevere un intero.");

            String trimmed = raw.trim();
            if(trimmed.isEmpty()) {
                System.out.println ("Nessun valore inserito");
                continue;
            }

            try{
                return Integer.parseInt(trimmed);
            }catch(NumberFormatException e){
                System.out.println("Valore non valido: inserisci un numero intero");
            }
        }


    }
    public boolean readBoolean(String prompt){
        while(true){
            String raw = readLine(prompt);
            if(raw == null) return false;

            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) continue;
            switch (normalized) {
                case "y", "yes", "true" , "t", "s", "si", "sì":
                    return true;
                case "n", "no", "false", "f":
                    return false;
                default:
                    System.out.println("Risposta non valida. Inserisci sì/no");
            }
        }
    }
    public List<String> readList(String prompt, String separator) {
        String line = readLine(prompt);
        if (line == null) return List.of();

    String effectiveSeparator = (separator == null || separator.isEmpty() )
            ? DEFAULT_SEPARATOR
            : separator;

    String[] rawValues = line.split(Pattern.quote(effectiveSeparator));

    List<String> values = new ArrayList<>(rawValues.length);
    for (String value : rawValues) {
        String trimmed = value.trim();
        if(!trimmed.isEmpty()) values.add(trimmed);
    }
    return values;
    }
}
