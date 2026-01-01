package it.unibs.ingsw24_25.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;


public class AvailabilitySubmissionPolicyTest {

    @Test
    void isWindowOpenReturnsTrueWithinSubmissionWindow() {
        YearMonth referenceMonth = YearMonth.of(2024, 11);
        LocalDate today = LocalDate.of(2024, 10, 10);

        boolean result = AvailabilitySubmissionPolicy.isWindowOpen(referenceMonth, today);

        assertThat(result).isTrue();
    }

    @Test
    void isWindowOpenReturnsFalseOutsideSubmissionWindow() {
        YearMonth referenceMonth = YearMonth.of(2024, 11);
        LocalDate today = LocalDate.of(2024, 10, 23);

        boolean result = AvailabilitySubmissionPolicy.isWindowOpen(referenceMonth, today);

        assertThat(result).isFalse();
    }

    @Test
    void strategyCanBeSwappedWithoutChangingCallers() {
        AvailabilitySubmissionPolicy.setStrategy(new AvailabilitySubmissionStrategy() {
            @Override
            public boolean isWindowOpen(YearMonth referenceMonth, LocalDate today) {
                return true;
            }

            @Override
            public YearMonth nextSubmissionMonth(LocalDate today) {
                return YearMonth.of(2099, 1);
            }
        });

        try {
            YearMonth referenceMonth = YearMonth.of(2024, 11);
            LocalDate today = LocalDate.of(2024, 10, 23);

            boolean result = AvailabilitySubmissionPolicy.isWindowOpen(referenceMonth, today);

            assertThat(result).isTrue();
            assertThat(AvailabilitySubmissionPolicy.nextSubmissionMonth(today))
                    .isEqualTo(YearMonth.of(2099, 1));
        } finally {
            AvailabilitySubmissionPolicy.resetStrategy();
        }
    }
}
