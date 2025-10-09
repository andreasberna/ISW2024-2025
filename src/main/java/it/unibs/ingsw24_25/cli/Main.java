package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.ConfiguratorServiceImp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final Path PLACES_FILE = DATA_DIRECTORY.resolve("places.json");
    private static final Path VISIT_TYPES_FILE = DATA_DIRECTORY.resolve("visit-types.json");
    private static final Path VOLUNTEERS_FILE = DATA_DIRECTORY.resolve("volunteers.json");
    private static final Path SETTINGS_FILE = DATA_DIRECTORY.resolve("settings.json");
    private static final Path CONFIGURATORS_FILE = DATA_DIRECTORY.resolve("configurators.json");

    public static void main(String[] args) {
        ConfiguratorService service = buildService();
        Printer printer = new Printer (System.out);
        PromptReader reader = new PromptReader (new Scanner (System.in));

        FirstAccessSetup setup = new FirstAccessSetup (service, reader, printer);
        setup.run();

        CommandHandler handler = new CommandHandler (service, printer, reader);
        CommandRouter router = new CommandRouter (handler);
        CliApp cliApp = new CliApp(reader, printer, router);
        cliApp.run();
    }

    private static ConfiguratorService buildService() {
        PlaceRepository placeRepository = new JSONPlaceRepository (PLACES_FILE);
        VisitTypeRepository visitTypeRepository = new JSONVisitTypeRepository (VISIT_TYPES_FILE);
        VolunteerRepository volunteerRepository = new JSONVolunteerRepository (VOLUNTEERS_FILE);
        SettingsRepository settingsRepository = new JSONSettingsRepository (SETTINGS_FILE);
        JSONConfiguratorRepository configuratorRepository = new JSONConfiguratorRepository (CONFIGURATORS_FILE);

        return new ConfiguratorServiceImp (
                placeRepository,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                configuratorRepository
        );

    }
}
