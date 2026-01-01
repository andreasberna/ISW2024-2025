package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class VisitTypeTest {

    @Test
    void updateStateInitializesToProposed() throws Exception {
        VisitType visitType = buildVisitType(2, 5);
        LocalDate today = LocalDate.of(2024, 1, 10);
        setField(visitType, "enrollmentDeadline", today.plusDays(2));

        visitType.updateState(today);

        assertThat(visitType.getState()).isEqualTo(VisitState.PROPOSTA);
    }

    @Test
    void updateStateMovesToCompleteWhenMaxReached() throws Exception {
        VisitType visitType = buildVisitType(2, 5);
        LocalDate today = LocalDate.of(2024, 1, 10);
        setField(visitType, "state", VisitState.PROPOSTA);
        setField(visitType, "enrolled", 5);
        setField(visitType, "enrollmentDeadline", today.plusDays(2));

        visitType.updateState(today);

        assertThat(visitType.getState()).isEqualTo(VisitState.COMPLETA);
    }

    @Test
    void updateStateConfirmsWhenDeadlineReachedAndMinMet() throws Exception {
        VisitType visitType = buildVisitType(2, 5);
        LocalDate today = LocalDate.of(2024, 1, 10);
        setField(visitType, "state", VisitState.PROPOSTA);
        setField(visitType, "enrolled", 2);
        setField(visitType, "enrollmentDeadline", today);

        visitType.updateState(today);

        assertThat(visitType.getState()).isEqualTo(VisitState.CONFERMATA);
    }

    @Test
    void updateStateCancelsWhenDeadlineReachedWithoutMin() throws Exception {
        VisitType visitType = buildVisitType(2, 5);
        LocalDate today = LocalDate.of(2024, 1, 10);
        setField(visitType, "state", VisitState.PROPOSTA);
        setField(visitType, "enrolled", 1);
        setField(visitType, "enrollmentDeadline", today);

        visitType.updateState(today);

        assertThat(visitType.getState()).isEqualTo(VisitState.CANCELLATA);
    }

    @Test
    void updateStateMovesFromCompleteToProposedBeforeDeadline() throws Exception {
        VisitType visitType = buildVisitType(3, 5);
        LocalDate today = LocalDate.of(2024, 1, 10);
        setField(visitType, "state", VisitState.COMPLETA);
        setField(visitType, "enrolled", 2);
        setField(visitType, "enrollmentDeadline", today.plusDays(1));

        visitType.updateState(today);

        assertThat(visitType.getState()).isEqualTo(VisitState.PROPOSTA);
    }

    @Test
    void updateStateMarksAsPerformedAfterVisitDay() throws Exception {
        VisitType visitType = buildVisitType(2, 5);
        LocalDate today = LocalDate.of(2024, 1, 10);
        setField(visitType, "state", VisitState.CONFERMATA);
        setField(visitType, "visitDate", today.minusDays(1));

        visitType.updateState(today);

        assertThat(visitType.getState()).isEqualTo(VisitState.EFFETTUATA);
    }

    private VisitType buildVisitType(int minParticipants, int maxParticipants) {
        VisitType visitType = new VisitType();
        visitType.setMinParticipants(minParticipants);
        visitType.setMaxParticipants(maxParticipants);
        return visitType;
    }

    private void setField(VisitType visitType, String name, Object value) throws Exception {
        Field field = VisitType.class.getDeclaredField (name);
        field.setAccessible (true);
        field.set (visitType, value);
    }
}
