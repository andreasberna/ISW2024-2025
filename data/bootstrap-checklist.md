# Stato della configurazione iniziale

Questa checklist riassume come il progetto soddisfa i requisiti individuati nella nota "Cosa manca ancora".

1. **Punto di ingresso dell'applicazione.** La classe [`Main`](../src/main/java/it/unibs/ingsw24_25/cli/Main.java) istanzia tutti i repository JSON, crea il `ConfiguratorServiceImp` e avvia la CLI dopo aver eseguito il setup iniziale.
2. **Persistenza su file.** Tutti i repository ricevono percorsi puntati alla cartella [`data/`](../data) che contiene i file JSON vuoti richiesti per la serializzazione. La logica di bootstrap si occupa di creare eventuali cartelle mancanti.
3. **Prima configurazione.** La classe [`FirstAccessSetup`](../src/main/java/it/unibs/ingsw24_25/cli/FirstAccessSetup.java) gestisce il flusso interattivo di primo accesso, verificando le credenziali di default, chiedendo quelle personali e configurando ambito territoriale e limite partecipanti.
4. **Inizializzazione del repository delle credenziali.** [`JSONConfiguratorRepository`](../src/main/java/it/unibs/ingsw24_25/repository/JSONConfiguratorRepository.java) inizializza sempre la cache in memoria e la popola dai dati su disco, evitando `NullPointerException` al primo salvataggio.

## File di persistenza `data/*.json`

I file JSON nella cartella [`data/`](../data) fungono da storage su disco per i vari repository:

- `configurators.json` conserva le credenziali dell'amministratore configuratore.
- `places.json` memorizza gli ambiti o sedi disponibili per le visite.
- `visit-types.json` mantiene l'elenco delle tipologie di visita configurate.
- `volunteers.json` salva i volontari registrati.
- `settings.json` contiene i parametri di configurazione generale (ambito territoriale, limite partecipanti, ecc.).

La cartella può essere tenuta accanto al file `pom.xml` (nella directory radice del progetto) così che l'applicazione, quando avviata dalla radice del progetto, trovi automaticamente i file di persistenza. In pratica la struttura minima deve risultare così:

```
ISW2024-2025/
├── pom.xml
├── data/
│   ├── configurators.json
│   ├── places.json
│   ├── settings.json
│   ├── visit-types.json
│   └── volunteers.json
└── src/
```
