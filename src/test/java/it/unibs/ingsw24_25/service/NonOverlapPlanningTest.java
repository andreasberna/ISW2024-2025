package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class NonOverlapPlanningTest {

    @Mock PlaceRepository placeRepository;
    @Mock VisitTypeRepository visitTypeRepository;
    @Mock VolunteerRepository volunteerRepository;
    @Mock SettingsRepository settingsRepository;
    @Mock MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    @Mock PlannedVisitRepository plannedVisitRepository;
    @Mock ConfiguratorCredentialManager credentialManager;

    @InjectMocks
    private ConfiguratorServiceImp service;

    private long countDayOfWeekInMonth(YearMonth month, DayOfWeek day) {
        long count = 0;
        for (LocalDate d = month.atDay(1); !d.isAfter(month.atEndOfMonth()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == day) count++;
        }
        return count;
    }

    @Test
    void generateMonthlyPlan_shouldNotScheduleOverlappingVisitsAtSamePlace() {
        Long planId = 1L;
        YearMonth targetMonth = YearMonth.now().plusMonths(2);

        // Same physical place
        Place place = new Place("Museo Civico", "Desc", "Via Roma 1");
        ReflectionTestUtils.setField(place, "id", "place-museum-1");

        // Two visit types at the SAME place on MONDAY with overlapping times:
        // vt1: 10:00 – 12:00  (120 min)
        // vt2: 11:00 – 12:00  (60 min)  ← overlaps with vt1
        VisitType vt1 = new VisitType("Tour Mattina", "Desc", "Ingresso Principale",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(2),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), 120)),
                false, 1, 10, place);
        VisitType vt2 = new VisitType("Tour Mezzogiorno", "Desc", "Ingresso Principale",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(2),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(11, 0), 60)),
                false, 1, 10, place);

        // Each volunteer qualifies for exactly one visit type
        Volunteer volunteer1 = new Volunteer("guide1", "pass");
        volunteer1.addVisit(vt1);
        volunteer1.registerAvailability(new MonthlyAvailability(targetMonth,
                Set.of(DayOfWeek.MONDAY), 10, LocalDate.now(), false, null));

        Volunteer volunteer2 = new Volunteer("guide2", "pass");
        volunteer2.addVisit(vt2);
        volunteer2.registerAvailability(new MonthlyAvailability(targetMonth,
                Set.of(DayOfWeek.MONDAY), 10, LocalDate.now(), false, null));

        MonthlyVisitPlan plan = new MonthlyVisitPlan(targetMonth);
        when(monthlyVisitPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(volunteerRepository.findAll()).thenReturn(List.of(volunteer1, volunteer2));
        when(visitTypeRepository.findAll()).thenReturn(List.of(vt1, vt2));

        service.generateMonthlyPlanVisits(planId);

        long mondaysInMonth = countDayOfWeekInMonth(targetMonth, DayOfWeek.MONDAY);

        // Only ONE visit should be generated per Monday (vt2 is blocked by vt1 overlap)
        assertThat(plan.getVisits())
                .hasSize((int) mondaysInMonth)
                .allMatch(v -> v.getVisitType().getVisitTitle().equals("Tour Mattina"));
    }

    @Test
    void generateMonthlyPlan_shouldScheduleNonOverlappingVisitsAtSamePlace() {
        Long planId = 2L;
        YearMonth targetMonth = YearMonth.now().plusMonths(2);

        Place place = new Place("Museo Civico", "Desc", "Via Roma 1");
        ReflectionTestUtils.setField(place, "id", "place-museum-2");

        // NON-overlapping: vt1 10:00-11:00, vt2 11:00-12:00
        VisitType vt1 = new VisitType("Tour Mattina", "Desc", "Ingresso",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(2),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), 60)),
                false, 1, 10, place);
        VisitType vt2 = new VisitType("Tour Pomeriggio", "Desc", "Ingresso",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(2),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(11, 0), 60)),
                false, 1, 10, place);

        Volunteer volunteer1 = new Volunteer("guide1", "pass");
        volunteer1.addVisit(vt1);
        volunteer1.registerAvailability(new MonthlyAvailability(targetMonth,
                Set.of(DayOfWeek.MONDAY), 10, LocalDate.now(), false, null));

        Volunteer volunteer2 = new Volunteer("guide2", "pass");
        volunteer2.addVisit(vt2);
        volunteer2.registerAvailability(new MonthlyAvailability(targetMonth,
                Set.of(DayOfWeek.MONDAY), 10, LocalDate.now(), false, null));

        MonthlyVisitPlan plan = new MonthlyVisitPlan(targetMonth);
        when(monthlyVisitPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(volunteerRepository.findAll()).thenReturn(List.of(volunteer1, volunteer2));
        when(visitTypeRepository.findAll()).thenReturn(List.of(vt1, vt2));

        service.generateMonthlyPlanVisits(planId);

        long mondaysInMonth = countDayOfWeekInMonth(targetMonth, DayOfWeek.MONDAY);

        // TWO visits per Monday since they don't overlap
        assertThat(plan.getVisits()).hasSize((int) mondaysInMonth * 2);
    }
}