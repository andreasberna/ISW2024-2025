package it.unibs.ingsw24_25.model;

import java.time.LocalDate;

public interface VisitStateBehavior {
    VisitState nextState(VisitType context, LocalDate today);
}
