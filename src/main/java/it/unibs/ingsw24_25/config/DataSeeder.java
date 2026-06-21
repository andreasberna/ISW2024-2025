package it.unibs.ingsw24_25.config;

import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ConfiguratorRepository configuratorRepository;
    private final VolunteerRepository volunteerRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final PlaceRepository placeRepository;
    private final VisitTypeRepository visitTypeRepository;
    private final MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    private final PlannedVisitRepository plannedVisitRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguratorService configuratorService;

    public DataSeeder(ConfiguratorRepository configuratorRepository,
                      VolunteerRepository volunteerRepository,
                      BeneficiaryRepository beneficiaryRepository,
                      PlaceRepository placeRepository,
                      VisitTypeRepository visitTypeRepository,
                      MonthlyVisitPlanRepository monthlyVisitPlanRepository,
                      PlannedVisitRepository plannedVisitRepository,
                      SettingsRepository settingsRepository,
                      PasswordEncoder passwordEncoder,
                      ConfiguratorService configuratorService) {
        this.configuratorRepository = configuratorRepository;
        this.volunteerRepository = volunteerRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.placeRepository = placeRepository;
        this.visitTypeRepository = visitTypeRepository;
        this.monthlyVisitPlanRepository = monthlyVisitPlanRepository;
        this.plannedVisitRepository = plannedVisitRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuratorService = configuratorService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (configuratorRepository.count() == 0) {
            log.info("Database vuoto, avvio del seeding...");

            // 1. Impostazioni di Sistema
            SystemSettings settings = new SystemSettings(1L, "Provincia di Brescia", 10, Collections.emptyList(), YearMonth.now().plusMonths(1), PlanningPhase.RACCOLTA_DISPONIBILITA, null);



            settings = settingsRepository.save(settings);

            // 2. Utenti
            Configurator admin = new Configurator("admin", passwordEncoder.encode("admin123"));
            admin = configuratorRepository.save(admin);

            // firstAccessPending = true di default: al primo login viene chiesto il cambio password
            Volunteer volunteer1 = new Volunteer("mario.volontario", passwordEncoder.encode("volontario123"));
            volunteer1 = volunteerRepository.save(volunteer1);

            Volunteer volunteer2 = new Volunteer("luigi.volontario", passwordEncoder.encode("volontario123"));
            volunteer2 = volunteerRepository.save(volunteer2);

            Beneficiary beneficiary1 = new Beneficiary("anna.beneficiario", passwordEncoder.encode("beneficiario123"), "Anna Bianchi");
            beneficiary1 = beneficiaryRepository.save(beneficiary1);

            Beneficiary beneficiary2 = new Beneficiary("marco.beneficiario", passwordEncoder.encode("beneficiario123"), "Marco Verdi");
            beneficiary2 = beneficiaryRepository.save(beneficiary2);

            // 3. Luoghi e Tipologie di Visita
            Place place1 = new Place("Castello Medioevale", "Un magnifico castello", "Via del Castello, 1, Brescia");
            place1 = placeRepository.save(place1);

            Place place2 = new Place("Museo Archeologico", "Museo ricco di reperti storici", "Piazza del Museo, 2, Brescia");
            place2 = placeRepository.save(place2);

            VisitType visitType1 = new VisitType("Tour delle Mura", "Un tour guidato delle mura storiche.", "Ingresso principale", LocalDate.now(), LocalDate.now().plusYears(1), List.of(new TimeSlot(DayOfWeek.SATURDAY, LocalTime.of(10, 0), 120)), true, 5, 20, place1);
            visitType1 = visitTypeRepository.save(visitType1);

            VisitType visitType2 = new VisitType("Visita Guidata Notturna", "Un'esperienza unica per scoprire il castello di notte.", "Punto informazioni", LocalDate.now(), LocalDate.now().plusYears(1), List.of(new TimeSlot(DayOfWeek.FRIDAY, LocalTime.of(21, 0), 60)), true, 10, 30, place1);
            visitType2 = visitTypeRepository.save(visitType2);

            VisitType visitType3 = new VisitType("I Segreti del Museo", "Un tour esclusivo con l'archeologo.", "Biglietteria", LocalDate.now(), LocalDate.now().plusYears(1), List.of(new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(15, 0), 180)), false, 2, 10, place2);
            visitType3 = visitTypeRepository.save(visitType3);

            // Link Volunteers to visits
            volunteer1.addVisit(visitType1);
            volunteer1.addVisit(visitType3);
            volunteer1 = volunteerRepository.save(volunteer1);

            volunteer2.addVisit(visitType2);
            volunteer2.addVisit(visitType3);
            volunteer2 = volunteerRepository.save(volunteer2);

            // 4. Piani Mensili e Generazione Algoritmica
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            MonthlyVisitPlan plan1 = new MonthlyVisitPlan(nextMonth);
            plan1.setPhase(PlanningPhase.RACCOLTA_DISPONIBILITA);
            plan1 = monthlyVisitPlanRepository.save(plan1);

            // Add Availabilities
            volunteer1.registerAvailability(new MonthlyAvailability(nextMonth, Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), 2, LocalDate.now(), false, null));
            volunteer1 = volunteerRepository.save(volunteer1);

            volunteer2.registerAvailability(new MonthlyAvailability(nextMonth, Set.of(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY), 2, LocalDate.now(), false, null));
            volunteer2 = volunteerRepository.save(volunteer2);

            // Generate Visits
            configuratorService.generateMonthlyPlanVisits(plan1.getId());

            // Publish the plan
            configuratorService.publishMonthlyPlan(plan1.getId());

            // Aggiunge visite demo in stati diversi (COMPLETA, CANCELLATA) per mostrare il filtraggio
            // al beneficiario: queste non appariranno nel catalogo delle prenotabili
            plan1 = monthlyVisitPlanRepository.findById(plan1.getId()).orElseThrow();
            PlannedVisit visitaCompleta = new PlannedVisit(plan1, visitType1, volunteer1,
                    nextMonth.atDay(8), LocalTime.of(10, 0));
            visitaCompleta.setStatus(VisitStatus.COMPLETA);
            plannedVisitRepository.save(visitaCompleta);

            PlannedVisit visitaCancellata = new PlannedVisit(plan1, visitType3, volunteer1,
                    nextMonth.atDay(14), LocalTime.of(15, 0));
            visitaCancellata.setStatus(VisitStatus.CANCELLATA);
            plannedVisitRepository.save(visitaCancellata);


            log.info("==================================================");
            log.info("      DATASEEDER COMPLETATO CON SUCCESSO       ");
            log.info("==================================================");
            log.info("Credenziali Demo:");
            log.info("  - Configuratore: admin / admin123");
            log.info("  - Volontario 1:  mario.volontario / volontario123  [CAMBIO PASSWORD richiesto al primo login]");
            log.info("  - Volontario 2:  luigi.volontario / volontario123  [CAMBIO PASSWORD richiesto al primo login]");
            log.info("  - Beneficiario 1: anna.beneficiario / beneficiario123");
            log.info("  - Beneficiario 2: marco.beneficiario / beneficiario123");
            log.info("Entità Create:");
            log.info("  - 1 Impostazione di Sistema (Ambito: Provincia di Brescia)");
            log.info("  - 2 Luoghi (Castello Medioevale, Museo Archeologico)");
            log.info("  - 3 Tipologie di Visita");
            log.info("  - 1 Piano Mensile Generato e Pubblicato per {}", nextMonth);
            log.info("==================================================");
        } else {
            log.info("Database non vuoto, seeding non necessario.");
        }
    }
}
