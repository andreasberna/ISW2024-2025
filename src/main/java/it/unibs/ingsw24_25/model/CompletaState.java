package it.unibs.ingsw24_25.model;

import java.time.LocalDate;

public class CompletaState implements VisitStateBehavior {
    @Override
    public VisitState nextState(VisitType context, LocalDate today) {
        LocalDate deadline = context.getEnrollmentDeadline();
        if (deadline != null && context.getEnrolled() < context.getMinParticipants() && today.isBefore(deadline)) {
            return VisitState.PROPOSTA;
        }
        if (deadline != null && today.isEqual(deadline)) {
            return context.getEnrolled() >= context.getMinParticipants()
                    ? VisitState.CONFERMATA
                    : VisitState.CANCELLATA;
        }
        return VisitState.COMPLETA;
    }
}
