package it.unibs.ingsw24_25.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class AvailabilitySubmissionPolicy {
    private static final AvailabilitySubmissionStrategy DEFAULT_STRATEGY =
            new StandardAvailabilitySubmissionStrategy();
    private static AvailabilitySubmissionStrategy strategy = DEFAULT_STRATEGY;
    private AvailabilitySubmissionPolicy() {
    }

    public static boolean isWindowOpen(YearMonth referenceMonth, LocalDate today) {
        return strategy.isWindowOpen(referenceMonth, today);
    }

    public static YearMonth nextSubmissionMonth(LocalDate today) {
        return strategy.nextSubmissionMonth(today);
    }
    public static void setStrategy(AvailabilitySubmissionStrategy newStrategy) {
        strategy = Objects.requireNonNull(newStrategy, "strategy non può essere nullo");
    }
    public static void resetStrategy() {
        strategy = DEFAULT_STRATEGY;
    }
}
