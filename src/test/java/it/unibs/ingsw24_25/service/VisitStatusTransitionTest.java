package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.*;
import it.unibs.ingsw24_25.repository.PlannedVisitRepository;
import it.unibs.ingsw24_25.repository.VisitTypeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitStatusTransitionTest {

    @Mock PlannedVisitRepository plannedVisitRepository;
    @Mock VisitTypeRepository visitTypeRepository;

    @InjectMocks
    VisitStatusUpdateService service;

    private PlannedVisit buildVisit(int minParticipants, int maxParticipants) {
        Place place = new Place("Museo", "Desc", "Via Roma");
        VisitType visitType = new VisitType("Visita Test", "Desc", "Ingresso",
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), 60)),
                false, minParticipants, maxParticipants, place);
        MonthlyVisitPlan plan = new MonthlyVisitPlan(YearMonth.now().plusMonths(1));
        Volunteer volunteer = new Volunteer("guide", "pass");
        return new PlannedVisit(plan, visitType, volunteer, LocalDate.now().plusDays(3), LocalTime.of(10, 0));
    }

    @Test
    void updateVisitStatuses_whenParticipantsMeetMinimum_shouldConfirm() {
        LocalDate checkDate = LocalDate.now().plusDays(3);
        PlannedVisit visit = buildVisit(2, 10);
        visit.addBooking(new VisitBooking("user1", "Mario Rossi", 3, null)); // 3 >= 2 min

        when(plannedVisitRepository.findByVisitDateAndStatus(checkDate, VisitStatus.PROPOSTA))
                .thenReturn(List.of(visit));

        service.updateVisitStatuses();

        assertThat(visit.getStatus()).isEqualTo(VisitStatus.CONFERMATA);
        verify(plannedVisitRepository).save(visit);
    }

    @Test
    void updateVisitStatuses_whenParticipantsBelowMinimum_shouldCancel() {
        LocalDate checkDate = LocalDate.now().plusDays(3);
        PlannedVisit visit = buildVisit(5, 10);
        visit.addBooking(new VisitBooking("user1", "Luigi Verdi", 2, null)); // 2 < 5 min

        when(plannedVisitRepository.findByVisitDateAndStatus(checkDate, VisitStatus.PROPOSTA))
                .thenReturn(List.of(visit));

        service.updateVisitStatuses();

        assertThat(visit.getStatus()).isEqualTo(VisitStatus.CANCELLATA);
        verify(plannedVisitRepository).save(visit);
    }

    @Test
    void updateVisitStatuses_whenNoParticipants_shouldCancel() {
        LocalDate checkDate = LocalDate.now().plusDays(3);
        PlannedVisit visit = buildVisit(1, 10); // min 1, but no bookings

        when(plannedVisitRepository.findByVisitDateAndStatus(checkDate, VisitStatus.PROPOSTA))
                .thenReturn(List.of(visit));

        service.updateVisitStatuses();

        assertThat(visit.getStatus()).isEqualTo(VisitStatus.CANCELLATA);
    }
}