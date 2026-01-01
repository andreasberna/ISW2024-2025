package it.unibs.ingsw24_25.model;

import java.time.LocalDate;

public class CancellataState implements VisitStateBehavior {
    @Override
    public VisitState nextState(VisitType context, LocalDate today) {
        return VisitState.CANCELLATA;
    }
}
