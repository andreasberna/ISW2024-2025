package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import it.unibs.ingsw24_25.service.ConfiguratorServiceImp;
import it.unibs.ingsw24_25.service.VolunteerService;
import it.unibs.ingsw24_25.service.VolunteerServiceImp;

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
    private static final Path MONTHLY_PLANS_FILE = DATA_DIRECTORY.resolve("monthly-plans.json");

    public static void main(String[] args) {
        MonthlyVisitPlanRepository monthlyVisitPlanRepository = new JSONMonthlyVisitPlanRepository (MONTHLY_PLANS_FILE);
        VolunteerRepository volunteerRepository = createVolunteerRepository(monthlyVisitPlanRepository);
        SettingsRepository settingsRepository = createSettingsRepository();
        VolunteerService volunteerService = new VolunteerServiceImp (volunteerRepository, settingsRepository);
        ConfiguratorService service = buildService (volunteerRepository, settingsRepository, monthlyVisitPlanRepository, volunteerService);
        Printer printer = new Printer (System.out);
        PromptReader reader = new PromptReader (new Scanner (System.in));

        FirstAccessSetup setup = new FirstAccessSetup (service, volunteerService, reader, printer);
        setup.run();

        ConfiguratorCommandHandler configuratorHandler = new ConfiguratorCommandHandler (service, printer, reader);
        VolunteerCommandHandler volunteerHandler = new VolunteerCommandHandler (volunteerService, setup, printer, reader);
        CliApp cliApp = new CliApp (reader, printer, configuratorHandler, volunteerHandler);
        cliApp.run();
    }


    private static ConfiguratorService buildService(VolunteerRepository volunteerRepository, SettingsRepository settingsRepository, MonthlyVisitPlanRepository monthlyVisitPlanRepository, VolunteerService volunteerService) {
        PlaceRepository placeRepository = new JSONPlaceRepository (PLACES_FILE, monthlyVisitPlanRepository);
        VisitTypeRepository visitTypeRepository = new JSONVisitTypeRepository (VISIT_TYPES_FILE, monthlyVisitPlanRepository);
        JSONConfiguratorRepository configuratorRepository = new JSONConfiguratorRepository (CONFIGURATORS_FILE);

        return new ConfiguratorServiceImp (
                placeRepository,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                monthlyVisitPlanRepository,
                configuratorRepository,
                volunteerService
        );

    }

    private static VolunteerRepository createVolunteerRepository(MonthlyVisitPlanRepository monthlyVisitPlanRepository) {
        return new JSONVolunteerRepository (VOLUNTEERS_FILE, monthlyVisitPlanRepository);
    }
    private static SettingsRepository createSettingsRepository() {
        return new JSONSettingsRepository (SETTINGS_FILE);
    }
}
