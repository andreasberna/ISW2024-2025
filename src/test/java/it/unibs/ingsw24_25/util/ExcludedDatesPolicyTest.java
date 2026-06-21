package it.unibs.ingsw24_25.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcludedDatesPolicyTest {

    @Test
    void allowedMonthAddsTwoMonthsBeforeWindowStart() {
        LocalDate reference = LocalDate.of(2024, 5, 10);

        YearMonth allowed = ExcludedDatePolicy.allowedMonth(reference);

        assertThat(allowed).isEqualTo(YearMonth.of(2024, 7));
    }

    @Test
    void allowedMonthAddsThreeMonthsWithinWindow() {
        LocalDate reference = LocalDate.of(2024, 5, 20);

        YearMonth allowed = ExcludedDatePolicy.allowedMonth(reference);

        assertThat(allowed).isEqualTo(YearMonth.of(2024, 8));
    }

    @Test
    void allowedMonthRejectsNullReference() {
        assertThatThrownBy(() -> ExcludedDatePolicy.allowedMonth(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reference date");
    }
}