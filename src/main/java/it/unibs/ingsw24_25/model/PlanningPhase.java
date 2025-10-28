package it.unibs.ingsw24_25.model;

public enum PlanningPhase {
    /** Fase iniziale: i volontari possono inviare o aggiornare le disponibilità. */
    AVAILABILITY_COLLECTION_OPEN,
    /** La finestra di raccolta è stata chiusa e le disponibilità sono congelate. */
    AVAILABILITY_COLLECTION_CLOSED,
    /** Il catalogo può essere modificato per il mese in pianificazione. */
    CATALOG_MANAGEMENT,
    /** Il piano è stato generato e può essere revisionato. */
    PLAN_GENERATED,
    /** I volontari vengono assegnati alle visite pianificate. */
    ASSIGNMENT,
    /** Ultima fase per eventuali rimozioni o modifiche prima della riapertura. */
    REVIEW,
    /** Il ciclo corrente è concluso ed è possibile riaprire la finestra di raccolta. */
    READY_FOR_NEXT_CYCLE;

    /**
     * Verifica che la transizione sia compatibile con l'ordine predefinito delle fasi.
     * @param next fase di destinazione
     * @return {@code true} se la transizione è consentita.
     */
    public boolean canTransitionTo(PlanningPhase next) {
        if (next == null) {
            return false;
        }
        if (this == next) {
            return true;
        }
        return this.ordinal() < next.ordinal();
    }

    /**
     * Restituisce la fase successiva secondo l'ordine naturale.
     * @return la fase successiva oppure {@code this} se già all'ultimo stato.
     */
    public PlanningPhase next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return this;
        }
        return values()[nextOrdinal];
    }
}
