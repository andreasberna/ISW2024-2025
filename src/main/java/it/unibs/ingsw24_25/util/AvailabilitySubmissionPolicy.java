package it.unibs.ingsw24_25.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class AvailabilitySubmissionPolicy {
    private AvailabilitySubmissionPolicy() {
    }

    public static boolean isWindowOpen(YearMonth referenceMonth, LocalDate today) {
        Objects.requireNonNull(referenceMonth, "referenceMonth non può essere nullo");
        Objects.requireNonNull(today, "today non può essere nullo");

        YearMonth submissionMonth = referenceMonth.minusMonths (1);
        LocalDate windowStart = submissionMonth.atDay (1);
        LocalDate deadline = submissionMonth.atDay (15);

        if (today.isBefore (windowStart)) {
            return false;
        }

        return !today.isAfter(deadline);
    }

    public static YearMonth nextSubmissionMonth(LocalDate today) {
        Objects.requireNonNull(today, "today non può essere nullo");
        LocalDate firstDayNextMonth = today.plusMonths(1).withDayOfMonth(1);
        return YearMonth.from(firstDayNextMonth);
    }
}
