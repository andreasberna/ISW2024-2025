package it.unibs.ingsw24_25.cli;

import java.util.*;
import java.util.regex.Pattern;

public class PromptReader {
    private static final String DEFAULT_SEPARATOR = ",";

    private Scanner scanner;

    public PromptReader(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "Scanner non può essere nullo");
    }

    public String readLine(String prompt) {
      while(true){
          System.out.print(prompt);
          try{
              if(!scanner.hasNextLine())
                  throw new IllegalStateException("Input terminato inaspettatamente");
              String line = scanner.nextLine();
              if(line == null) continue;

              String trimmed = line.trim();
              if(trimmed.isEmpty()){
                  System.out.println("Inserire valore non vuoto.");
                  continue;
              }
              return trimmed;
          }catch(IllegalStateException e){
              System.out.println("Errore di lettura, riprovare.");
          }catch(NoSuchElementException e){
              System.out.println("Input non disponibile, riprovare.");
          }
      }
    }

    public boolean readBoolean(String prompt){
        while(true){
            String value = readLine(prompt).toLowerCase(Locale.ITALY);
            if("si".equals (value) || "s".equals (value) || "y".equals (value) || "yes".equals (value)){
                return true;
            } ;
            if("no".equals (value) || "n".equals (value)){
                return false;
            }
            System.out.println("Risposta non valida. Inserire si/no.");
        }
    }

    public List<String> readValues(String prompt, String separator){
        String effectiveSeparator = separator == null || separator.isEmpty () ? DEFAULT_SEPARATOR : separator;
        String input = readLine(prompt);
        String[] rawValues = input.split(effectiveSeparator);
        List<String> values = new ArrayList<>();
        Arrays.stream(rawValues)
                .map (String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(values::add);
        return values;
    }

}
