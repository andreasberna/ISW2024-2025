package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguratorServiceImpTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private SettingsRepository settingsRepository;
    @Mock
    private ConfiguratorCredentialManager credentialManager;
    @Mock
    private MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private VisitTypeRepository visitTypeRepository;
    @Mock
    private PlannedVisitRepository plannedVisitRepository;

    @InjectMocks
    private ConfiguratorServiceImp service;

    @Test
    void testGenerateMonthlyPlanVisits() {
        // Arrange
        Long planId = 1L;
        YearMonth targetMonth = YearMonth.now().plusMonths(1);
        MonthlyVisitPlan plan = new MonthlyVisitPlan(targetMonth);
        when(monthlyVisitPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        Volunteer volunteer = new Volunteer("test", "test");
        volunteer.registerAvailability(new MonthlyAvailability(targetMonth, Set.of(DayOfWeek.MONDAY), 1, LocalDate.now(), false, null));
        when(volunteerRepository.findAll()).thenReturn(List.of(volunteer));

        Place place = new Place("Test Place", "Test Description", "Test Location");
        VisitType visitType = new VisitType("Test Visit", "Test Description", "Test Location",
                LocalDate.now(), LocalDate.now().plusYears(1),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), 60)),
                false, 1, 10, place);
        volunteer.addVisit(visitType);
        when(visitTypeRepository.findAll()).thenReturn(List.of(visitType));

        long mondaysInMonth = 0;
        for (LocalDate day = targetMonth.atDay(1); !day.isAfter(targetMonth.atEndOfMonth()); day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.MONDAY) {
                mondaysInMonth++;
            }
        }

        // Act
        service.generateMonthlyPlanVisits(planId);

        // Assert
        assertThat(plan.getPhase()).isEqualTo(PlanningPhase.PIANIFICAZIONE_COMPLETATA);
        assertThat(plan.getVisits()).hasSize((int) mondaysInMonth);
    }
}
