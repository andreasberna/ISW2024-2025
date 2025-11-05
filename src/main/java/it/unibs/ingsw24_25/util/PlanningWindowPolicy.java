package it.unibs.ingsw24_25.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class PlanningWindowPolicy {
    private PlanningWindowPolicy() {
        // utility class
    }

    /**
     * Restituisce il mese per cui dovrà essere raccolta la prossima disponibilità.
     * Prima del giorno 16 del mese corrente la finestra si riferisce al mese successivo,
     * dal 16 in poi alla successiva finestra (due mesi in avanti).
     *
     * @param today data odierna
     * @return il mese di pianificazione associato alla prossima finestra di raccolta
     */
    public static YearMonth resolveNextPlanningMonth(LocalDate today) {
        Objects.requireNonNull(today, "today non può essere nullo");
        YearMonth baseline = YearMonth.from(today);
        if (today.getDayOfMonth() >= 16) {
            return baseline.plusMonths(2);
        }
        return baseline.plusMonths(1);
    }
}
