package it.unibs.ingsw24_25.model;

import java.time.LocalDate;

public class PropostaState implements VisitStateBehavior {
    @Override
    public VisitState nextState(VisitType context, LocalDate today) {
        LocalDate deadline = context.getEnrollmentDeadline();
        if (context.getEnrolled() == context.getMaxParticipants()) {
            return VisitState.COMPLETA;
        }
        if (deadline != null && today.isEqual(deadline)) {
            return context.getEnrolled() >= context.getMinParticipants()
                    ? VisitState.CONFERMATA
                    : VisitState.CANCELLATA;
        }
        return VisitState.PROPOSTA;
    }
}
