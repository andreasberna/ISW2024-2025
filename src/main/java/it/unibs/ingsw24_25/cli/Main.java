package it.unibs.ingsw24_25.cli;

import it.unibs.ingsw24_25.controller.BeneficiaryController;
import it.unibs.ingsw24_25.controller.ConfiguratorController;
import it.unibs.ingsw24_25.controller.VolunteerController;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.service.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final Path PLACES_FILE = DATA_DIRECTORY.resolve("places.json");
    private static final Path VISIT_TYPES_FILE = DATA_DIRECTORY.resolve("visit-types.json");
    private static final Path VOLUNTEERS_FILE = DATA_DIRECTORY.resolve("volunteers.json");
    private static final Path BENEFICIARY_FILE = DATA_DIRECTORY.resolve("beneficiaries.json");
    private static final Path SETTINGS_FILE = DATA_DIRECTORY.resolve("settings.json");
    private static final Path CONFIGURATORS_FILE = DATA_DIRECTORY.resolve("configurators.json");
    private static final Path MONTHLY_PLANS_FILE = DATA_DIRECTORY.resolve("monthly-plans.json");
    private static final Path PROVISIONED_CREDENTIALS_FILE = DATA_DIRECTORY.resolve("provisioned-credentials.json");

    public static void main(String[] args) {
        MonthlyVisitPlanRepository monthlyVisitPlanRepository = new JSONMonthlyVisitPlanRepository (MONTHLY_PLANS_FILE);
        VolunteerRepository volunteerRepository = createVolunteerRepository(monthlyVisitPlanRepository);
        SettingsRepository settingsRepository = createSettingsRepository();
        VisitTypeRepository visitTypeRepository = new  JSONVisitTypeRepository (VISIT_TYPES_FILE, monthlyVisitPlanRepository);
        ProvisionedCredentialsRepository provisionedCredentialsRepository = new JSONProvisionedCredentialsRepository (PROVISIONED_CREDENTIALS_FILE);
        VolunteerService volunteerService = new VolunteerServiceImp (volunteerRepository, settingsRepository,
                monthlyVisitPlanRepository, visitTypeRepository, provisionedCredentialsRepository);
        ConfiguratorService service = buildService (volunteerRepository, settingsRepository, monthlyVisitPlanRepository,
                provisionedCredentialsRepository, volunteerService, visitTypeRepository);
        BeneficiaryService beneficiaryService = buildBeneficiaryService(BENEFICIARY_FILE, volunteerRepository, monthlyVisitPlanRepository,
                visitTypeRepository, settingsRepository);
        ConfiguratorController configuratorController = new ConfiguratorController (service);
        VolunteerController volunteerController = new VolunteerController (volunteerService);
        BeneficiaryController beneficiaryController = new BeneficiaryController (beneficiaryService);
        Printer printer = new Printer (System.out);
        PromptReader reader = new PromptReader (new Scanner (System.in));

        FirstAccessService firstAccessService = new FirstAccessService (service, volunteerService);
        FirstAccessSetup setup = new FirstAccessSetup (firstAccessService, reader, printer);

        ConfiguratorCommandHandler configuratorHandler = new ConfiguratorCommandHandler (configuratorController, setup, printer, reader);
        VolunteerCommandHandler volunteerHandler = new VolunteerCommandHandler (volunteerController, setup, printer, reader);
        BeneficiaryCommandHandler beneficiaryHandler = new BeneficiaryCommandHandler (beneficiaryController, printer, reader);

        CliApp cliApp = new CliApp (reader, printer, configuratorHandler, volunteerHandler, beneficiaryHandler);
        cliApp.run();
    }


    private static ConfiguratorService buildService(VolunteerRepository volunteerRepository,
                                                    SettingsRepository settingsRepository,
                                                    MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                                    ProvisionedCredentialsRepository provisionedCredentialsRepository,
                                                    VolunteerService volunteerService,
                                                    VisitTypeRepository visitTypeRepository) {
        PlaceRepository placeRepository = new JSONPlaceRepository (PLACES_FILE, monthlyVisitPlanRepository);
        JSONConfiguratorRepository configuratorRepository = new JSONConfiguratorRepository (CONFIGURATORS_FILE);

        return new ConfiguratorServiceImp (
                placeRepository,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                monthlyVisitPlanRepository,
                configuratorRepository,
                provisionedCredentialsRepository,
                volunteerService
        );

    }

    private static BeneficiaryService buildBeneficiaryService(Path beneficiariesFile,
                                                              VolunteerRepository volunteerRepository,
                                                              MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                                                              VisitTypeRepository visitTypeRepository,
                                                              SettingsRepository settingsRepository) {
        BeneficiaryRepository beneficiaryRepository = new JSONBeneficiaryRepository(beneficiariesFile);
        return new BeneficiaryServiceImp(
                beneficiaryRepository,
                volunteerRepository,
                monthlyVisitPlanRepository,
                visitTypeRepository,
                settingsRepository
        );
    }


    private static VolunteerRepository createVolunteerRepository(MonthlyVisitPlanRepository monthlyVisitPlanRepository) {
        return new JSONVolunteerRepository (VOLUNTEERS_FILE, monthlyVisitPlanRepository);
    }
    private static SettingsRepository createSettingsRepository() {
        return new JSONSettingsRepository (SETTINGS_FILE);
    }
}
