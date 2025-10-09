package it.unibs.ingsw24_25.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class ExcludedDatePolicy {

    private ExcludedDatePolicy() {}

    public static YearMonth allowedMonth(LocalDate referenceDate){
        LocalDate today = Objects.requireNonNull (referenceDate, "reference date non può essere nullo");
        YearMonth currentMonth = YearMonth.from(today);

        LocalDate windowStart = currentMonth.atDay(16);
        LocalDate windowEnd = currentMonth.plusMonths (1).atDay(15);

        if(!today.isBefore (windowStart) && !today.isAfter(windowEnd)){
            return currentMonth.plusMonths (3);
        }

        return currentMonth.plusMonths (2);
    }
}
