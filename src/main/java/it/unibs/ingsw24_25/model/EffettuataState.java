package it.unibs.ingsw24_25.model;

import java.time.LocalDate;

public class EffettuataState implements VisitStateBehavior{
    @Override
    public VisitState nextState(VisitType context, LocalDate today) {
        return VisitState.EFFETTUATA;
    }
}
