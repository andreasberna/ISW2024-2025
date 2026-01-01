package it.unibs.ingsw24_25.util;

import java.time.LocalDate;
import java.time.YearMonth;

public interface AvailabilitySubmissionStrategy {
    boolean isWindowOpen(YearMonth referenceMonth, LocalDate today);

    YearMonth nextSubmissionMonth(LocalDate today);
}
