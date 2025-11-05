package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.Configurator;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.MonthlyVisitPlan;
import it.unibs.ingsw24_25.model.Place;
import it.unibs.ingsw24_25.model.PlanningPhase;
import it.unibs.ingsw24_25.model.SystemSettings;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;
import it.unibs.ingsw24_25.service.VolunteerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguratorServiceImpTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private VisitTypeRepository visitTypeRepository;
    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private SettingsRepository settingsRepository;
    @Mock
    private ConfiguratorRepository configuratorRepository;
    @Mock
    private MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    @Mock
    private VolunteerService volunteerService;
    @Mock
    private ProvisionedCredentialsRepository  provisionedCredentialsRepository;

    private ConfiguratorServiceImp service;

    @BeforeEach
    void setUp() {
        service = new ConfiguratorServiceImp(
                placeRepository,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                monthlyVisitPlanRepository,
                configuratorRepository,
                provisionedCredentialsRepository,
                volunteerService
        );
        lenient().when(settingsRepository.load()).thenReturn(Optional.empty());
        lenient ().when (provisionedCredentialsRepository.hasConfiguratorCredential (anyString())).thenReturn (false);
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThatThrownBy(() -> new ConfiguratorServiceImp(
                null,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                monthlyVisitPlanRepository,
                configuratorRepository,
                provisionedCredentialsRepository,
                volunteerService
        )).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new ConfiguratorServiceImp(
                placeRepository,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                monthlyVisitPlanRepository,
                configuratorRepository,
                null,
                volunteerService
        )).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new ConfiguratorServiceImp(
                placeRepository,
                visitTypeRepository,
                volunteerRepository,
                settingsRepository,
                monthlyVisitPlanRepository,
                configuratorRepository,
                provisionedCredentialsRepository,
                null
        )).isInstanceOf(NullPointerException.class);
    }

    private SystemSettings settingsWithPhase(PlanningPhase phase) {
        return new SystemSettings(
                "scope",
                10,
                List.of(),
                YearMonth.of(2024, 1),
                phase,
                LocalDate.of(2023, 12, 1)
        );
    }

    private VisitType visitType(Place place, String id, String title) {
        return new VisitType(
                id,
                title,
                "Descrizione",
                "Ingresso",
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 12, 31),
                List.of(),
                false,
                1,
                10,
                place,
                new ArrayList<>()
        );
    }

    @Nested
    @DisplayName("Default credentials workflow")
    class DefaultCredentials {
        @Test
        void verifyDefaultCredentialsSucceedsWhenPendingAndMatching() {
            when(provisionedCredentialsRepository.hasConfiguratorCredential("config")).thenReturn(true);
            when(provisionedCredentialsRepository.findConfiguratorPassword("config")).thenReturn(Optional.of("psswrd"));

            assertDoesNotThrow(() -> service.verifyDefaultCredentials("config", "psswrd"));
        }

        @Test
        void verifyDefaultCredentialsFailsIfNotPending() {
            when(provisionedCredentialsRepository.hasConfiguratorCredential("config")).thenReturn(false);

            assertThatThrownBy(() -> service.verifyDefaultCredentials("config", "psswrd"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("già state impostate");
        }

        @Test
        void verifyDefaultCredentialsFailsIfValuesDoNotMatch() {
            when(provisionedCredentialsRepository.hasConfiguratorCredential("config")).thenReturn(true);
            when(provisionedCredentialsRepository.findConfiguratorPassword("config")).thenReturn(Optional.of("psswrd"));

            assertThatThrownBy(() -> service.verifyDefaultCredentials("comnfig", "wrong"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non valide");
        }

        @Test
        void setPersonalCredentialsPersistsSanitizedValuesAfterValidation() {
            when(provisionedCredentialsRepository.hasConfiguratorCredential("config")).thenReturn(true);
            when(provisionedCredentialsRepository.findConfiguratorPassword("config")).thenReturn(Optional.of("psswrd"));
            service.verifyDefaultCredentials("config", "psswrd");

            service.setPersonalCredentials("config", "  admin  ", "  secret  ");

            ArgumentCaptor<Configurator> captor = ArgumentCaptor.forClass(Configurator.class);
            verify(configuratorRepository).save(captor.capture());
            Configurator saved = captor.getValue();
            assertThat(saved.getNickname()).isEqualTo("admin");
            assertThat(saved.getPassword()).isEqualTo("secret");
            verify (provisionedCredentialsRepository).consumeConfiguratorCredential ("config");
        }

        @Test
        void setPersonalCredentialsFailsIfDefaultNotValidated() {
            when(provisionedCredentialsRepository.hasConfiguratorCredential("config")).thenReturn(true);


            assertThatThrownBy(() -> service.setPersonalCredentials("config", "nick", "pwd"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non ancora verificate");

        }

        @Test
        void verifyLoginReturnsTrueForStoredCredentials() {
            Configurator configurator = new Configurator("admin", "secret");
            when(configuratorRepository.load()).thenReturn(Optional.of(Map.of("admin", configurator)));

            boolean result = service.verifyLogin(" admin ", " secret ");

            assertThat(result).isTrue();
        }

        @Test
        void verifyLoginReturnsFalseWhenFirstAccessPending() {
            when (provisionedCredentialsRepository.hasConfiguratorCredential("admin")).thenReturn(true);

            boolean result = service.verifyLogin("admin", "secret");

            assertThat(result).isFalse();
        }

        @Test
        void hasPendingConfiguratorSeedsDelegatesToRepository() {
            when(provisionedCredentialsRepository.hasAnyConfiguratorCredential()).thenReturn(true);

            assertThat(service.hasPendingConfiguratorSeeds()).isTrue();
            verify(provisionedCredentialsRepository).hasAnyConfiguratorCredential();
        }
    }

    @Nested
    @DisplayName("System settings management")
    class SystemSettingsManagement {

        @Test
        void setTerritorialScopeCreatesSettingsWhenMissing() {
            when(settingsRepository.load()).thenReturn(Optional.empty());

            service.setTerritorialScope("Brescia");

            ArgumentCaptor<SystemSettings> captor = ArgumentCaptor.forClass(SystemSettings.class);
            verify(settingsRepository).save(captor.capture());
            SystemSettings saved = captor.getValue();
            assertThat(saved.getTerritorialScope()).isEqualTo("Brescia");
            assertThat(saved.getMaxPeoplePerSubscription()).isEqualTo(15);
            assertThat(saved.getExcludedDates()).isEmpty();
        }

        @Test
        void setTerritorialScopeIsIdempotentWhenSameValueAlreadyPresent() {
            SystemSettings existing = new SystemSettings("Brescia", 10, List.of());
            when(settingsRepository.load()).thenReturn(Optional.of(existing));

            service.setTerritorialScope("Brescia");

        }

        @Test
        void setTerritorialScopeThrowsIfDifferentValueAlreadyPresent() {
            SystemSettings existing = new SystemSettings("Brescia", 10, List.of());
            when(settingsRepository.load()).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.setTerritorialScope("Milano"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non è modificabile");
        }

        @Test
        void setMaxPeoplePerSubscriptionUpdatesExistingSettings() {
            SystemSettings existing = new SystemSettings("Brescia", 10, List.of());
            when(settingsRepository.load()).thenReturn(Optional.of(existing));

            service.setMaxPeoplePerSubscription(20);

            ArgumentCaptor<SystemSettings> captor = ArgumentCaptor.forClass(SystemSettings.class);
            verify(settingsRepository).save(captor.capture());
            assertThat(captor.getValue().getMaxPeoplePerSubscription()).isEqualTo(20);
        }

        @Test
        void setMaxPeoplePerSubscriptionFailsWhenSettingsMissing() {
            when(settingsRepository.load()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setMaxPeoplePerSubscription(20))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("impostare prima l'ambito territoriale");
        }

        @Test
        void setBlackoutDatesSanitizesAndPersistsValues() {
            YearMonth allowedMonth = ExcludedDatePolicy.allowedMonth(LocalDate.now());
            LocalDate day1 = allowedMonth.atDay(1);
            LocalDate day2 = allowedMonth.atDay(2);
            LocalDate duplicate = allowedMonth.atDay(1);

            SystemSettings settings = new SystemSettings("scope", 5, new ArrayList<>());
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            service.setBlackoutDates(new ArrayList<> (Arrays.asList (day2, duplicate, null, day1, duplicate)));

            ArgumentCaptor<SystemSettings> captor = ArgumentCaptor.forClass(SystemSettings.class);
            verify(settingsRepository).save(captor.capture());
            SystemSettings saved = captor.getValue();
            assertThat(saved.getExcludedDates()).containsExactly(day1, day2);
        }

        @Test
        void setBlackoutDatesRejectsValuesOutsideAllowedMonth() {
            YearMonth allowedMonth = ExcludedDatePolicy.allowedMonth(LocalDate.now());
            LocalDate invalid = allowedMonth.plusMonths(1).atDay(1);
            SystemSettings settings = new SystemSettings("scope", 5, new ArrayList<>());
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.setBlackoutDates(List.of(invalid)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("devono appartenere");
        }
    }

    @Nested
    @DisplayName("Place management")
    class PlaceManagement {

        @Test
        void addPlaceStoresNewPlaceAndReturnsMessage() {
            when(placeRepository.findById("Museo")).thenReturn(Optional.empty());

            String message = service.addPlace("Museo", "descrizione", "Brescia");

            assertThat(message).isEqualTo("Luogo inserito: Museo");
            ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
            verify(placeRepository).save(captor.capture());
            Place saved = captor.getValue();
            assertThat(saved.getPlaceTitle()).isEqualTo("Museo");
            assertThat(saved.getPlaceDescription()).isEqualTo("descrizione");
            assertThat(saved.getLocation()).isEqualTo("Brescia");
            assertThat(saved.getVisits()).isEmpty();
        }

        @Test
        void addPlaceRejectsDuplicateNames() {
            when(placeRepository.findById("Museo")).thenReturn(Optional.of(new Place("Museo", "d", "l")));

            assertThatThrownBy(() -> service.addPlace("Museo", "descrizione", "Brescia"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("esiste già");
        }

        @Test
        void addPlaceRejectsWhenCatalogWindowNotYetOpen() {
            SystemSettings settings = new SystemSettings(
                    "scope",
                    10,
                    List.of(),
                    YearMonth.of(2024, 1),
                    PlanningPhase.AVAILABILITY_COLLECTION_CLOSED,
                    null
            );
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.addPlace("Museo", "descrizione", "Brescia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("modifiche al catalogo sono consentite solo dopo la generazione del piano");
        }

        @Test
        void addPlaceRejectsWhenAvailabilityCollectionAlreadyReopened() {
            SystemSettings settings = new SystemSettings(
                    "scope",
                    10,
                    List.of(),
                    YearMonth.of(2024, 1),
                    PlanningPhase.READY_FOR_NEXT_CYCLE,
                    null
            );
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.addPlace("Museo", "descrizione", "Brescia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Le modifiche al catalogo non sono consentite dopo la riapertura");
        }
    }

    @Nested
    @DisplayName("Visit type management")
    class VisitTypeManagement {
        @Test
        void addVisitTypeCreatesLinkWithPlace() {
            Place place = new Place("Museo", "descrizione", "Brescia");
            when(placeRepository.findById("Museo")).thenReturn(Optional.of(place));

            List<TimeSlot> schedules = List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), Duration.ofHours(2)));

            try (MockedConstruction<VisitType> mocked = mockConstruction(VisitType.class, (mockVisit, context) -> {
                when(mockVisit.getVisitTitle()).thenReturn("Visita");
            })) {
                String message = service.addVisitType(
                        "Museo",
                        "Visita",
                        "Descrizione",
                        "Ingresso",
                        schedules,
                        true,
                        5,
                        10,
                        LocalDate.now(),
                        LocalDate.now().plusDays(10)
                );

                assertThat(message).startsWith("Visita inserita: Visita (ID: ");
                VisitType constructed = mocked.constructed().get(0);
                verify(visitTypeRepository).save(constructed);
                assertThat(place.getVisits()).containsExactly(constructed);
                verify(placeRepository).save(place);
            }
        }

        @Test
        void addVisitTypeValidatesParticipantsBounds() {
            assertThatThrownBy(() -> service.addVisitType(
                    "Museo",
                    "Visita",
                    "Descrizione",
                    "Ingresso",
                    List.of(),
                    false,
                    10,
                    5,
                    LocalDate.now(),
                    LocalDate.now().plusDays(10)
            )).isInstanceOf(IllegalArgumentException.class);

        }

        @Test
        void addVisitTypeRejectsStartDateNotAfterActiveMonthWhenWindowClosed() {
            SystemSettings settings = new SystemSettings(
                    "scope",
                    10,
                    List.of(),
                    YearMonth.of(2024, 1),
                    PlanningPhase.PLAN_GENERATED,
                    null
            );
            when(settingsRepository.load()).thenReturn(Optional.of(settings));


            assertThatThrownBy(() -> service.addVisitType(
                    "Museo",
                    "Visita",
                    "Descrizione",
                    "Ingresso",
                    List.of(),
                    false,
                    5,
                    10,
                    LocalDate.of(2024, 1, 10),
                    LocalDate.of(2024, 3, 10)
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dopo la chiusura");

        }
    }

    @Nested
    @DisplayName("Volunteer management")
    class VolunteerManagement {

        @Test
        void addVolunteerPersistsVolunteerWithEmptyVisits() {
            when(volunteerRepository.findByNickname("alice")).thenReturn(Optional.empty());

            service.addVolunteer("alice", "password");

            ArgumentCaptor<Volunteer> captor = ArgumentCaptor.forClass(Volunteer.class);
            verify(volunteerRepository).save(captor.capture());
            Volunteer saved = captor.getValue();
            assertThat(saved.getNickname()).isEqualTo("alice");
            assertThat(saved.getVisitsAttending()).isEmpty();
        }

        @Test
        void addVolunteerRejectsExistingNickname() {
            Volunteer existing = new Volunteer("alice", "password");
            when(volunteerRepository.findByNickname("alice")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.addVolunteer("alice", "password"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("già presente");
        }

        @Test
        void linkVolunteerToVisitCreatesBidirectionalRelation() {
            Volunteer volunteer = new Volunteer("alice", "password");
            VisitType visitType = mock(VisitType.class);
            List<Volunteer> guides = new ArrayList<>();

            when(volunteerRepository.findByNickname("alice")).thenReturn(Optional.of(volunteer));
            when(visitTypeRepository.findById("Visita")).thenReturn(Optional.of(visitType));
            when(visitType.getGuides()).thenReturn(guides);
            doAnswer(invocation -> {
                Volunteer guide = invocation.getArgument(0);
                guides.add(guide);
                return null;
            }).when(visitType).addGuide(any(Volunteer.class));

            service.linkVOlunteerToVisit("alice", "Visita");

            assertThat(volunteer.getVisitsAttending()).containsExactly(visitType);
            assertThat (guides).containsExactly (volunteer);
            verify(visitType).addGuide(volunteer);
            verify(volunteerRepository).save(volunteer);
            verify(visitTypeRepository).save(visitType);
        }
    }

    @Nested
    @DisplayName("Listing operations")
    class Listing {
        @Test
        void listPlaceReturnsMappedDto() {
            Place place = new Place("Museo", "descrizione", "Brescia");
            when(placeRepository.findAll()).thenReturn(List.of(place));

            List<PlaceDTO> dtos = service.listPlace();

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getTitle()).isEqualTo("Museo");
        }

        @Test
        void listVisitTypeByPlaceValidatesInputAndDelegatesToRepository() {
            Place place = new Place("Museo", "descrizione", "Brescia");
            when(placeRepository.findById("Museo")).thenReturn(Optional.of(place));
            VisitType visit = mock(VisitType.class);
            when(visit.getVisitTitle()).thenReturn("Visita");
            when(visit.getSchedules()).thenReturn(List.of());
            when(visit.getTicketRequired()).thenReturn(false);
            when(visit.getMinParticipants()).thenReturn(1);
            when(visit.getMaxParticipants()).thenReturn(5);
            when(visit.getPlace()).thenReturn(place);
            when(visit.getState()).thenReturn(null);
            when(visitTypeRepository.findByPlace("Museo")).thenReturn(List.of(visit));

            List<VisitTypeDTO> dtos = service.listVisitTypeByPlace("Museo");

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getTitle()).isEqualTo("Visita");
        }

        @Test
        void listVisitTypeReturnsAllVisits() {
            VisitType visit = mock(VisitType.class);
            when(visit.getVisitTitle()).thenReturn("Visita");
            when(visit.getSchedules()).thenReturn(List.of());
            when(visit.getTicketRequired()).thenReturn(false);
            when(visit.getMinParticipants()).thenReturn(1);
            when(visit.getMaxParticipants()).thenReturn(5);
            when(visit.getPlace()).thenReturn(new Place("Museo", "d", "l"));
            when(visit.getState()).thenReturn(null);
            when(visitTypeRepository.findAll()).thenReturn(List.of(visit));

            List<VisitTypeDTO> dtos = service.listVisitType();

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getTitle()).isEqualTo("Visita");
        }

        @Test
        void listVolunteerWithVisitTypeReturnsMappedDto() {
            Volunteer volunteer = new Volunteer("alice", "password");
            VisitType visit = mock(VisitType.class);
            when(visit.getVisitTitle()).thenReturn("Visita");
            volunteer.addVisit(visit);
            when(volunteerRepository.findAll()).thenReturn(List.of(volunteer));

            List<VolunteerDTO> dtos = service.listVolunteerWVisitType();

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getNickname()).isEqualTo("alice");
            assertThat(dtos.get(0).getVisitTypeTitles()).containsExactly("Visita");
        }
    }
    private VisitType stubVisitType(String id, String title, Place place, List<Volunteer> guides) {
        VisitType visit = mock(VisitType.class);
        when(visit.getId()).thenReturn(id);
        when(visit.getVisitTitle()).thenReturn(title);
        when(visit.getPlace()).thenReturn(place);
        when(visit.getGuides()).thenAnswer(invocation -> guides);
        return visit;
    }

    @Nested
    @DisplayName("Catalog removal cascades")
    class CatalogRemovalCascades {

        @Test
        void removePlaceRejectsWhenPlanNotGeneratedYet() {
            SystemSettings settings = settingsWithPhase(PlanningPhase.AVAILABILITY_COLLECTION_CLOSED);
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.removePlace("Museo"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("modifiche al catalogo sono consentite solo dopo la generazione del piano");

        }

        @Test
        void removeVolunteerRejectsWhenAvailabilityCollectionAlreadyReopened() {
            SystemSettings settings = settingsWithPhase(PlanningPhase.READY_FOR_NEXT_CYCLE);
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.removeVolunteer("alice"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Le modifiche al catalogo non sono consentite dopo la riapertura");

        }

        @Test
        void removeVisitTypeRejectsWhenPlanNotGeneratedYet() {
            SystemSettings settings = settingsWithPhase(PlanningPhase.AVAILABILITY_COLLECTION_CLOSED);
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            assertThatThrownBy(() -> service.removeVisitType("visit-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("modifiche al catalogo sono consentite solo dopo la generazione del piano");

        }

        @Test
        void removePlaceDeletesVisitTypesAndAssignmentsBeforeDeletingPlace() {
            SystemSettings settings = new SystemSettings (
                    "scope",
                    10,
                    List.of (),
                    YearMonth.of (2024, 1),
                    PlanningPhase.PLAN_GENERATED,
                    null
            );
            when (settingsRepository.load ()).thenReturn (Optional.of (settings), Optional.of (settings));

            Place place = new Place ("Museo", "descrizione", "Brescia");
            VisitType visitOne = visitType(place, "visit-1", "Visita 1");
            VisitType visitTwo = visitType(place, "visit-2", "Visita 2");
            place.setVisits (new ArrayList<> (List.of (visitOne, visitTwo)));

            when (placeRepository.findById ("Museo")).thenReturn (Optional.of (place));
            when (visitTypeRepository.findByPlace ("Museo")).thenReturn (List.of (visitOne, visitTwo));
            when (volunteerRepository.findAll ()).thenReturn (List.of ());

            service.removePlace ("Museo");

            verify (visitTypeRepository).deleteById ("visit-1");
            verify (visitTypeRepository).deleteById ("visit-2");

            ArgumentCaptor<Iterable> idCaptor = ArgumentCaptor.forClass (Iterable.class);
            verify (monthlyVisitPlanRepository).removePlannedVisitsByVisitTypes (idCaptor.capture ());
            assertThat (idCaptor.getValue ()).containsExactlyInAnyOrder ("visit-1", "visit-2");

            verify (placeRepository).deleteById ("Museo");
        }

        @Test
        void removeVolunteerPurgesOrphanedVisitTypeAndPlace() {
            SystemSettings settings = new SystemSettings(
                    "scope",
                    10,
                    List.of(),
                    YearMonth.of(2024, 1),
                    PlanningPhase.PLAN_GENERATED,
                    null
            );
            when(settingsRepository.load()).thenReturn(Optional.of(settings), Optional.of(settings));

            Place place = new Place("Museo", "descrizione", "Brescia");
            VisitType visit = visitType(place, "visit-1", "Visita");
            place.setVisits(new ArrayList<>(List.of(visit)));

            Volunteer volunteer = new Volunteer("alice", "password");
            volunteer.addVisit(visit);
            visit.addGuide (volunteer);

            when(volunteerRepository.findByNickname("alice")).thenReturn(Optional.of(volunteer));
            when(volunteerRepository.findAll()).thenReturn(List.of());
            when(visitTypeRepository.findById("visit-1")).thenReturn(Optional.of(visit));
            when(placeRepository.findById("Museo")).thenReturn(Optional.of(place));

            service.removeVolunteer("alice");

            verify(monthlyVisitPlanRepository).removeVolunteerAssignments("alice");
            verify(volunteerService).removeVolunteerAccount("alice");
            verify(monthlyVisitPlanRepository).removePlannedVisitsByVisitType("visit-1");
            verify(visitTypeRepository).deleteById("visit-1");
            verify(placeRepository).deleteById("Museo");
        }

        @Test
        void removeVisitTypeCascadesToVolunteerAndPlaceRemovalWhenOrphaned() {
            SystemSettings settings = new SystemSettings(
                    "scope",
                    10,
                    List.of(),
                    YearMonth.of(2024, 1),
                    PlanningPhase.PLAN_GENERATED,
                    null
            );
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            Place place = new Place("Museo", "descrizione", "Brescia");

            VisitType visit = visitType (place, "visit-1", "Visita");
            place.setVisits(new ArrayList<>(List.of(visit)));

            Volunteer volunteer = new Volunteer("alice", "password");
            volunteer.addVisit(visit);
            visit.addGuide (volunteer);

            when(visitTypeRepository.findById("visit-1")).thenReturn(Optional.of(visit));
            when(visitTypeRepository.findAll()).thenReturn(List.of(visit));
            when(placeRepository.findById("Museo")).thenReturn(Optional.of(place));
            when(volunteerRepository.findAll()).thenReturn(List.of(volunteer));

            service.removeVisitType("visit-1");

            verify(volunteerService).removeVolunteerAccount("alice");
            verify(monthlyVisitPlanRepository).removeVolunteerAssignments("alice");
            verify(monthlyVisitPlanRepository).removePlannedVisitsByVisitType("visit-1");
            verify(visitTypeRepository).deleteById("visit-1");
            verify(placeRepository).deleteById("Museo");
        }
    }

    @Nested
    @DisplayName("Planning cycle management")
    class PlanningCycleManagement {

        @Test
        void reopenAvailabilityWindowClearsSnapshotsAndVolunteerAvailabilities() {
            YearMonth activeMonth = YearMonth.of(2024, 1);
            SystemSettings settings = new SystemSettings(
                    "scope",
                    10,
                    List.of(),
                    activeMonth,
                    PlanningPhase.REVIEW,
                    LocalDate.of(2023, 12, 10)
            );
            when(settingsRepository.load()).thenReturn(Optional.of(settings));

            MonthlyAvailability snapshot = new MonthlyAvailability(
                    activeMonth,
                    EnumSet.of(DayOfWeek.MONDAY),
                    1,
                    LocalDate.of(2023, 12, 5),
                    true,
                    LocalDate.of(2023, 12, 12)
            );
            MonthlyVisitPlan plan = new MonthlyVisitPlan(
                    activeMonth,
                    PlanningPhase.ASSIGNMENT,
                    LocalDate.of(2023, 12, 15),
                    List.of(),
                    Map.of("alice", snapshot)
            );
            when(monthlyVisitPlanRepository.findByMonth(activeMonth)).thenReturn(Optional.of(plan));

            Volunteer volunteer = new Volunteer("alice", "password");
            MonthlyAvailability januaryAvailability = new MonthlyAvailability(
                    activeMonth,
                    EnumSet.of(DayOfWeek.MONDAY),
                    1,
                    LocalDate.of(2023, 12, 5)
            );
            volunteer.registerAvailability(januaryAvailability, LocalDate.of(2023, 12, 5));
            when(volunteerRepository.findAll()).thenReturn(List.of(volunteer));

            LocalDate today = LocalDate.of(2023, 12, 20);
            service.reopenAvailabilityWindow(today);

            assertThat(plan.getPhase()).isEqualTo(PlanningPhase.READY_FOR_NEXT_CYCLE);
            assertThat(plan.getAvailabilitySnapshots()).isEmpty();
            verify(monthlyVisitPlanRepository).save(plan);

            assertThat(volunteer.findAvailability(activeMonth)).isEmpty();
            verify(volunteerRepository).save(volunteer);

            assertThat(settings.getActivePlanningMonth()).isEqualTo(YearMonth.of(2024, 2));
            assertThat(settings.getPlanningPhase()).isEqualTo(PlanningPhase.AVAILABILITY_COLLECTION_OPEN);
            verify(settingsRepository).save(settings);
        }
    }

    @Nested
    @DisplayName("Configurator account management")
    class ConfiguratorAccountManagement {

        @Test
        void registerConfiguratorPersistsSanitizedValuesWhenAvailable() {
            when(configuratorRepository.load()).thenReturn(Optional.of(Map.of("admin", new Configurator("admin", "secret"))));

            service.registerConfigurator("  planner  ", "  password  ");

            ArgumentCaptor<Configurator> captor = ArgumentCaptor.forClass(Configurator.class);
            verify(configuratorRepository).save(captor.capture());
            Configurator stored = captor.getValue();
            assertThat(stored.getNickname()).isEqualTo("planner");
            assertThat(stored.getPassword()).isEqualTo("password");
        }

        @Test
        void registerConfiguratorFailsWhenNicknameBlank() {
            assertThatThrownBy(() -> service.registerConfigurator("   ", "password"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nickname");
        }

        @Test
        void registerConfiguratorFailsWhenNicknameAlreadyExistsIgnoringCase() {
            when(configuratorRepository.load()).thenReturn(Optional.of(Map.of("Planner", new Configurator("Planner", "pwd"))));

            assertThatThrownBy(() -> service.registerConfigurator("planner", "password"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Esiste già");
        }

        @Test
        void listConfiguratorsReturnsSortedNicknames() {
            Map<String, Configurator> stored = new HashMap<>();
            stored.put("gamma", new Configurator("gamma", "pwd"));
            stored.put("alpha", new Configurator("alpha", "pwd"));
            stored.put("Beta", new Configurator("Beta", "pwd"));
            when(configuratorRepository.load()).thenReturn(Optional.of(stored));

            List<String> result = service.listConfigurators();

            assertThat(result).containsExactly("alpha", "Beta", "gamma");
        }
    }
}
