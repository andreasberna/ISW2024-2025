package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import it.unibs.ingsw24_25.util.ExcludedDatePolicy;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                volunteerService
        );
        lenient().when(settingsRepository.load()).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("Default credentials workflow")
    class DefaultCredentials {
        @Test
        void verifyDefaultCredentialsSucceedsWhenPendingAndMatching() {
            when(configuratorRepository.exists()).thenReturn(false);

            assertDoesNotThrow(() -> service.verifyDefaultCredentials("config", "psswrd"));
        }

        @Test
        void verifyDefaultCredentialsFailsIfNotPending() {
            when(configuratorRepository.exists()).thenReturn(true);

            assertThatThrownBy(() -> service.verifyDefaultCredentials("config", "psswrd"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("già state impostate");
        }

        @Test
        void verifyDefaultCredentialsFailsIfValuesDoNotMatch() {
            when(configuratorRepository.exists()).thenReturn(false);

            assertThatThrownBy(() -> service.verifyDefaultCredentials("wrong", "psswrd"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non valide");
        }

        @Test
        void setPersonalCredentialsPersistsSanitizedValuesAfterValidation() {
            when(configuratorRepository.exists()).thenReturn(false);
            service.verifyDefaultCredentials("config", "psswrd");

            service.setPersonalCredentials("config", "  admin  ", "  secret  ");

            ArgumentCaptor<Configurator> captor = ArgumentCaptor.forClass(Configurator.class);
            verify(configuratorRepository).save(captor.capture());
            Configurator saved = captor.getValue();
            assertThat(saved.getNickname()).isEqualTo("admin");
            assertThat(saved.getPassword()).isEqualTo("secret");
        }

        @Test
        void setPersonalCredentialsFailsIfDefaultNotValidated() {
            when(configuratorRepository.exists()).thenReturn(false);

            assertThatThrownBy(() -> service.setPersonalCredentials("config", "nick", "pwd"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non ancora verificate");

            verify(configuratorRepository, never()).save(any());
        }

        @Test
        void verifyLoginReturnsTrueForStoredCredentials() {
            when(configuratorRepository.exists()).thenReturn(true);
            Configurator configurator = new Configurator("admin", "secret");
            when(configuratorRepository.load()).thenReturn(Optional.of(Map.of("admin", configurator)));

            boolean result = service.verifyLogin(" admin ", " secret ");

            assertThat(result).isTrue();
        }

        @Test
        void verifyLoginReturnsFalseWhenFirstAccessPending() {
            when(configuratorRepository.exists()).thenReturn(false);

            boolean result = service.verifyLogin("admin", "secret");

            assertThat(result).isFalse();
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

            verify(settingsRepository, never()).save(any());
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
            when (placeRepository.findById ("Museo")).thenReturn (Optional.empty ());

            String message = service.addPlace ("Museo", "descrizione", "Brescia");

            assertThat (message).isEqualTo ("Luogo inserito: Museo");
            ArgumentCaptor<Place> captor = ArgumentCaptor.forClass (Place.class);
            verify (placeRepository).save (captor.capture ());
            Place saved = captor.getValue ();
            assertThat (saved.getPlaceTitle ()).isEqualTo ("Museo");
            assertThat (saved.getPlaceDescription ()).isEqualTo ("descrizione");
            assertThat (saved.getLocation ()).isEqualTo ("Brescia");
            assertThat (saved.getVisits ()).isEmpty ();
        }

        @Test
        void addPlaceRejectsDuplicateNames() {
            when (placeRepository.findById ("Museo")).thenReturn (Optional.of (new Place ("Museo", "d", "l")));

            assertThatThrownBy (() -> service.addPlace ("Museo", "descrizione", "Brescia"))
                    .isInstanceOf (IllegalArgumentException.class)
                    .hasMessageContaining ("esiste già");
        }

        @Test
        void addPlaceRejectsWhenCatalogWindowNotYetOpen() {
            SystemSettings settings = new SystemSettings (
                    "scope",
                    10,
                    List.of (),
                    YearMonth.of (2024, 1),
                    PlanningPhase.AVAILABILITY_COLLECTION_CLOSED,
                    null
            );
            when (settingsRepository.load ()).thenReturn (Optional.of (settings));
            when (placeRepository.findById ("Museo")).thenReturn (Optional.empty ());

            assertThatThrownBy (() -> service.addPlace ("Museo", "descrizione", "Brescia"))
                    .isInstanceOf (IllegalStateException.class)
                    .hasMessageContaining ("modifiche al catalogo sono consentite solo dopo la generazione del piano");
        }

        @Test
        void addPlaceRejectsWhenAvailabilityCollectionAlreadyReopened() {
            SystemSettings settings = new SystemSettings (
                    "scope",
                    10,
                    List.of (),
                    YearMonth.of (2024, 1),
                    PlanningPhase.READY_FOR_NEXT_CYCLE,
                    null
            );
            when (settingsRepository.load ()).thenReturn (Optional.of (settings));
            when (placeRepository.findById ("Museo")).thenReturn (Optional.empty ());

            assertThatThrownBy (() -> service.addPlace ("Museo", "descrizione", "Brescia"))
                    .isInstanceOf (IllegalStateException.class)
                    .hasMessageContaining ("Le modifiche al catalogo non sono consentite dopo la riapertura");
        }

        @Nested
        @DisplayName("Visit type management")
        class VisitTypeManagement {
            @Test
            void addVisitTypeCreatesLinkWithPlace() {
                Place place = new Place ("Museo", "descrizione", "Brescia");
                when (placeRepository.findById ("Museo")).thenReturn (Optional.of (place));
                when (visitTypeRepository.findById ("Visita"))
                        .thenReturn (Optional.empty ());
                List<TimeSlot> schedules = List.of (new TimeSlot (DayOfWeek.MONDAY, LocalTime.of (10, 0), Duration.ofHours (2)));

                try (MockedConstruction<VisitType> mocked = mockConstruction (VisitType.class, (mockVisit, context) -> {
                    when (mockVisit.getVisitTitle ()).thenReturn ("Visita");
                })) {
                    String message = service.addVisitType (
                            "Museo",
                            "Visita",
                            "Descrizione",
                            "Ingresso",
                            schedules,
                            true,
                            5,
                            10,
                            LocalDate.now (),
                            LocalDate.now ().plusDays (10)
                    );

                    assertThat (message).isEqualTo ("Visita inserita: Visita");
                    VisitType constructed = mocked.constructed ().get (0);
                    verify (visitTypeRepository).save (constructed);
                    assertThat (place.getVisits ()).containsExactly (constructed);
                    verify (placeRepository).save (place);
                }
            }

            @Test
            void addVisitTypeValidatesParticipantsBounds() {
                assertThatThrownBy (() -> service.addVisitType (
                        "Museo",
                        "Visita",
                        "Descrizione",
                        "Ingresso",
                        List.of (),
                        false,
                        10,
                        5,
                        LocalDate.now (),
                        LocalDate.now ().plusDays (10)
                )).isInstanceOf (IllegalArgumentException.class);

                verifyNoInteractions (placeRepository);
                verifyNoInteractions (visitTypeRepository);
            }
        }

        @Nested
        @DisplayName("Volunteer management")
        class VolunteerManagement {

            @Test
            void addVolunteerPersistsVolunteerWithEmptyVisits() {
                when (volunteerRepository.findByNickname ("alice")).thenReturn (Optional.empty ());

                service.addVolunteer ("alice", "password");

                ArgumentCaptor<Volunteer> captor = ArgumentCaptor.forClass (Volunteer.class);
                verify (volunteerRepository).save (captor.capture ());
                Volunteer saved = captor.getValue ();
                assertThat (saved.getNickname ()).isEqualTo ("alice");
                assertThat (saved.getVisitsAttending ()).isEmpty ();
            }

            @Test
            void addVolunteerRejectsExistingNickname() {
                Volunteer existing = new Volunteer ("alice", "password");
                when (volunteerRepository.findByNickname ("alice")).thenReturn (Optional.of (existing));

                assertThatThrownBy (() -> service.addVolunteer ("alice", "password"))
                        .isInstanceOf (IllegalArgumentException.class)
                        .hasMessageContaining ("già presente");
            }

            @Test
            void linkVolunteerToVisitCreatesBidirectionalRelation() {
                Volunteer volunteer = new Volunteer ("alice", "password");
                VisitType visitType = mock (VisitType.class);
                List<Volunteer> guides = new ArrayList<> ();

                when (volunteerRepository.findByNickname ("alice")).thenReturn (Optional.of (volunteer));
                when (visitTypeRepository.findById ("Visita")).thenReturn (Optional.of (visitType));
                when (visitType.getGuides ()).thenReturn (guides);
                doAnswer (invocation -> {
                    Volunteer guide = invocation.getArgument (0);
                    guides.add (guide);
                    return null;
                }).when (visitType).addGuide (any (Volunteer.class));

                service.linkVOlunteerToVisit ("alice", "Visita");

                assertThat (volunteer.getVisitsAttending ()).containsExactly (visitType);
                assertThat (guides).containsExactly (volunteer);
                verify (visitType).addGuide (volunteer);
                verify (volunteerRepository).save (volunteer);
                verify (visitTypeRepository).save (visitType);
            }
        }

        @Nested
        @DisplayName("Listing operations")
        class Listing {
            @Test
            void listPlaceReturnsMappedDto() {
                Place place = new Place ("Museo", "descrizione", "Brescia");
                when (placeRepository.findAll ()).thenReturn (List.of (place));

                List<PlaceDTO> dtos = service.listPlace ();

                assertThat (dtos).hasSize (1);
                assertThat (dtos.get (0).getTitle ()).isEqualTo ("Museo");
            }

            @Test
            void listVisitTypeByPlaceValidatesInputAndDelegatesToRepository() {
                Place place = new Place ("Museo", "descrizione", "Brescia");
                when (placeRepository.findById ("Museo")).thenReturn (Optional.of (place));
                VisitType visit = mock (VisitType.class);
                when (visit.getVisitTitle ()).thenReturn ("Visita");
                when (visit.getSchedules ()).thenReturn (List.of ());
                when (visit.getTicketRequired ()).thenReturn (false);
                when (visit.getMinParticipants ()).thenReturn (1);
                when (visit.getMaxParticipants ()).thenReturn (5);
                when (visit.getPlace ()).thenReturn (place);
                when (visit.getState ()).thenReturn (null);
                when (visitTypeRepository.findByPlace ("Museo")).thenReturn (List.of (visit));

                List<VisitTypeDTO> dtos = service.listVisitTypeByPlace ("Museo");

                assertThat (dtos).hasSize (1);
                assertThat (dtos.get (0).getTitle ()).isEqualTo ("Visita");
            }

            @Test
            void listVisitTypeReturnsAllVisits() {
                VisitType visit = mock (VisitType.class);
                when (visit.getVisitTitle ()).thenReturn ("Visita");
                when (visit.getSchedules ()).thenReturn (List.of ());
                when (visit.getTicketRequired ()).thenReturn (false);
                when (visit.getMinParticipants ()).thenReturn (1);
                when (visit.getMaxParticipants ()).thenReturn (5);
                when (visit.getPlace ()).thenReturn (new Place ("Museo", "d", "l"));
                when (visit.getState ()).thenReturn (null);
                when (visitTypeRepository.findAll ()).thenReturn (List.of (visit));

                List<VisitTypeDTO> dtos = service.listVisitType ();

                assertThat (dtos).hasSize (1);
                assertThat (dtos.get (0).getTitle ()).isEqualTo ("Visita");
            }

            @Test
            void listVolunteerWithVisitTypeReturnsMappedDto() {
                Volunteer volunteer = new Volunteer ("alice", "password");
                VisitType visit = mock (VisitType.class);
                when (visit.getVisitTitle ()).thenReturn ("Visita");
                volunteer.addVisit (visit);
                when (volunteerRepository.findAll ()).thenReturn (List.of (volunteer));

                List<VolunteerDTO> dtos = service.listVolunteerWVisitType ();

                assertThat (dtos).hasSize (1);
                assertThat (dtos.get (0).getNickname ()).isEqualTo ("alice");
                assertThat (dtos.get (0).getVisitTypeTitles ()).containsExactly ("Visita");
            }
        }

        @Nested
        @DisplayName("Catalog removal cascades")
        class CatalogRemovalCascades {

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
                VisitType visitOne = new VisitType (
                        "visit-1",
                        "Visita 1",
                        "Descrizione",
                        "Ingresso",
                        LocalDate.of (2024, 2, 1),
                        LocalDate.of (2024, 12, 31),
                        List.of (),
                        false,
                        1,
                        10,
                        place,
                        new ArrayList<> ()
                );
                VisitType visitTwo = new VisitType (
                        "visit-2",
                        "Visita 2",
                        "Descrizione",
                        "Ingresso",
                        LocalDate.of (2024, 2, 1),
                        LocalDate.of (2024, 12, 31),
                        List.of (),
                        false,
                        1,
                        10,
                        place,
                        new ArrayList<> ()
                );
                place.setVisits (new ArrayList<> (List.of (visitOne, visitTwo)));

                when (placeRepository.findById ("Museo")).thenReturn (Optional.of (place));
                when (visitTypeRepository.findByPlace ("Museo")).thenReturn (List.of (visitOne, visitTwo));
                when (placeRepository.findById (place.getPlaceTitle ())).thenReturn (Optional.of (place));
                when (volunteerRepository.findAll ()).thenReturn (List.of ());

                service.removePlace ("Museo");

                verify (visitTypeRepository).deleteById ("visit-1");
                verify (visitTypeRepository).deleteById ("visit-2");

                ArgumentCaptor<Iterable> idCaptor = ArgumentCaptor.forClass (Iterable.class);
                verify (monthlyVisitPlanRepository).removePlannedVisitsByVisitTypes (idCaptor.capture ());
                assertThat (idCaptor.getValue ()).containsExactlyInAnyOrder ("visit-1", "visit-2");

                verify (placeRepository).deleteById ("Museo");
                verifyNoInteractions (volunteerService);
            }

            @Test
            void removeVolunteerPurgesOrphanedVisitTypeAndPlace() {
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
                VisitType visit = new VisitType (
                        "visit-1",
                        "Visita",
                        "Descrizione",
                        "Ingresso",
                        LocalDate.of (2024, 2, 1),
                        LocalDate.of (2024, 12, 31),
                        List.of (),
                        false,
                        1,
                        10,
                        place,
                        new ArrayList<> ()
                );
                place.setVisits (new ArrayList<> (List.of (visit)));

                Volunteer volunteer = new Volunteer ("alice", "password");
                volunteer.addVisit (visit);
                visit.addGuide (volunteer);

                when (volunteerRepository.findByNickname ("alice")).thenReturn (Optional.of (volunteer));
                when (volunteerRepository.findAll ()).thenReturn (List.of ());
                when (visitTypeRepository.findById ("visit-1")).thenReturn (Optional.of (visit));
                when (placeRepository.findById ("Museo")).thenReturn (Optional.of (place));

                service.removeVolunteer ("alice");

                verify (monthlyVisitPlanRepository).removeVolunteerAssignments ("alice");
                verify (volunteerService).removeVolunteerAccount ("alice");
                verify (monthlyVisitPlanRepository).removePlannedVisitsByVisitType ("visit-1");
                verify (visitTypeRepository).deleteById ("visit-1");
                verify (placeRepository).deleteById ("Museo");
            }
        }

        @Nested
        @DisplayName("Planning cycle management")
        class PlanningCycleManagement {

            @Test
            void reopenAvailabilityWindowClearsSnapshotsAndVolunteerAvailabilities() {
                YearMonth activeMonth = YearMonth.of (2024, 1);
                SystemSettings settings = new SystemSettings (
                        "scope",
                        10,
                        List.of (),
                        activeMonth,
                        PlanningPhase.REVIEW,
                        LocalDate.of (2023, 12, 10)
                );
                when (settingsRepository.load ()).thenReturn (Optional.of (settings));

                MonthlyAvailability snapshot = new MonthlyAvailability (
                        activeMonth,
                        EnumSet.of (DayOfWeek.MONDAY),
                        1,
                        LocalDate.of (2023, 12, 5),
                        true,
                        LocalDate.of (2023, 12, 12)
                );
                MonthlyVisitPlan plan = new MonthlyVisitPlan (
                        activeMonth,
                        PlanningPhase.ASSIGNMENT,
                        LocalDate.of (2023, 12, 15),
                        List.of (),
                        Map.of ("alice", snapshot)
                );
                when (monthlyVisitPlanRepository.findByMonth (activeMonth)).thenReturn (Optional.of (plan));

                Volunteer volunteer = new Volunteer ("alice", "password");
                MonthlyAvailability januaryAvailability = new MonthlyAvailability (
                        activeMonth,
                        EnumSet.of (DayOfWeek.MONDAY),
                        1,
                        LocalDate.of (2023, 12, 5)
                );
                volunteer.registerAvailability (januaryAvailability, LocalDate.of (2023, 12, 5));
                when (volunteerRepository.findAll ()).thenReturn (List.of (volunteer));

                LocalDate today = LocalDate.of (2023, 12, 20);
                service.reopenAvailabilityWindow (today);

                assertThat (plan.getPhase ()).isEqualTo (PlanningPhase.READY_FOR_NEXT_CYCLE);
                assertThat (plan.getAvailabilitySnapshots ()).isEmpty ();
                verify (monthlyVisitPlanRepository).save (plan);

                assertThat (volunteer.findAvailability (activeMonth)).isEmpty ();
                verify (volunteerRepository).save (volunteer);

                assertThat (settings.getActivePlanningMonth ()).isEqualTo (YearMonth.of (2024, 2));
                assertThat (settings.getPlanningPhase ()).isEqualTo (PlanningPhase.AVAILABILITY_COLLECTION_OPEN);
                verify (settingsRepository).save (settings);
            }
        }
    }
}