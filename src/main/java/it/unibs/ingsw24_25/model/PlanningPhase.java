package it.unibs.ingsw24_25.model;

public enum PlanningPhase {
    RACCOLTA_DISPONIBILITA,
    PIANIFICAZIONE_COMPLETATA,
    PUBBLICATO;

    public boolean canTransitionTo(PlanningPhase next) {
        if (this == next) return true;
        if (next == null) return false;
        switch (this) {
            case RACCOLTA_DISPONIBILITA:
                return next == PIANIFICAZIONE_COMPLETATA;
            case PIANIFICAZIONE_COMPLETATA:
                return next == PUBBLICATO;
            case PUBBLICATO:
                return false;
            default:
                return false;
        }
    }
}
