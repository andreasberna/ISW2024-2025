package it.unibs.ingsw24_25.model;

import java.time.LocalDate;

public class ConfermataState implements VisitStateBehavior{
    @Override
    public VisitState nextState(VisitType context, LocalDate today) {
        LocalDate visitDay = context.getVisitDate();
        if (visitDay == null || !today.isEqual(visitDay)) {
            return VisitState.EFFETTUATA;
        }
        return VisitState.CONFERMATA;
    }
}
