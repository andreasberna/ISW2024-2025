package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.Beneficiary;
import it.unibs.ingsw24_25.model.MonthlyVisitPlan;
import it.unibs.ingsw24_25.model.PlannedVisit;
import it.unibs.ingsw24_25.model.SystemSettings;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.VisitBooking;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.repository.BeneficiaryRepository;
import it.unibs.ingsw24_25.repository.MonthlyVisitPlanRepository;
import it.unibs.ingsw24_25.repository.SettingsRepository;
import it.unibs.ingsw24_25.repository.VisitTypeRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceImpTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    @Mock
    private VisitTypeRepository visitTypeRepository;
    @Mock
    private SettingsRepository settingsRepository;

    private BeneficiaryServiceImp service;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryServiceImp(
                beneficiaryRepository,
                volunteerRepository,
                monthlyVisitPlanRepository,
                visitTypeRepository,
                settingsRepository
        );
        lenient().when(settingsRepository.load()).thenReturn(Optional.of(new SystemSettings("scope", 5, List.of())));
    }

    @Test
    void registerRejectsBlankUsername() {
        assertThatThrownBy(() -> service.register("Mario Rossi", "   ", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");

    }

    @Test
    void registerRejectsExistingUsernameAcrossRepositories() {
        when(beneficiaryRepository.findByUsername("nickname")).thenReturn(Optional.empty());
        when(volunteerRepository.findByNickname("nickname")).thenReturn(Optional.of(mock(it.unibs.ingsw24_25.model.Volunteer.class)));

        assertThatThrownBy(() -> service.register("Mario Rossi", "nickname", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già utilizzato");

    }

    @Test
    void registerPersistsSanitizedBeneficiary() {
        when(beneficiaryRepository.findByUsername("nickname")).thenReturn(Optional.empty());
        when(volunteerRepository.findByNickname("nickname")).thenReturn(Optional.empty());

        service.register("  Mario Rossi  ", "  nickname  ", "  secret  ");

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(beneficiaryRepository).save(captor.capture());
        Beneficiary saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("nickname");
        assertThat(saved.getFullName()).isEqualTo("Mario Rossi");
    }

    @Test
    void bookVisitRejectsParticipantsAboveSubscriptionLimit() {
        Beneficiary beneficiary = mock(Beneficiary.class);
        when(beneficiaryRepository.findByUsername("user")).thenReturn(Optional.of(beneficiary));
        when(settingsRepository.load()).thenReturn(Optional.of(new SystemSettings("scope", 3, List.of())));

        assertThatThrownBy(() -> service.bookVisit(" user ", "visit-1", 4, "note", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Numero partecipanti non valido");

    }

    @Test
    void bookVisitCreatesBookingAndPersistsPlan() {
        LocalDate today = LocalDate.now();
        Beneficiary beneficiary = mock(Beneficiary.class);
        when(beneficiary.getUsername()).thenReturn("user");
        when(beneficiary.getFullName()).thenReturn("User Name");
        when(beneficiaryRepository.findByUsername("user")).thenReturn(Optional.of(beneficiary));

        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, Duration.ofHours(2));
        PlannedVisit plannedVisit = new PlannedVisit("visit-123", today.plusDays(7), slot, "TYPE1", true, List.of(), VisitStatus.PROPOSED, List.of());
        MonthlyVisitPlan plan = new MonthlyVisitPlan(YearMonth.from(today.plusMonths(1)));
        plan.setPlannedVisits(List.of(plannedVisit));
        when(monthlyVisitPlanRepository.findAll()).thenReturn(List.of(plan));

        VisitType visitType = mock(VisitType.class);
        when(visitTypeRepository.findById("TYPE1")).thenReturn(Optional.of(visitType));
        when(visitType.getMaxParticipants()).thenReturn(10);
        when(visitType.getMinParticipants()).thenReturn(1);

        String bookingCode = service.bookVisit("user", "visit-123", 2, "note", today);

        assertThat(bookingCode).isNotBlank();
        PlannedVisit updatedVisit = plan.getPlannedVisits().get(0);
        assertThat(updatedVisit.getBookings()).hasSize(1);
        assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.CONFIRMED);
        verify(monthlyVisitPlanRepository).save(plan);
    }

    @Test
    void listVisitsByStatusSkipsNullPlansAndVisits() {
        VisitType visitType = mock(VisitType.class);
        when(visitType.getId()).thenReturn("TYPE1");
        when(visitType.getVisitTitle()).thenReturn("Title");
        when(visitType.getVisitDescription()).thenReturn("Description");
        when(visitType.getVisitMeetLocation()).thenReturn("Location");
        when(visitType.getTicketRequired()).thenReturn(Boolean.FALSE);
        when(visitType.getMinParticipants()).thenReturn(1);
        when(visitType.getMaxParticipants()).thenReturn(5);
        when(visitTypeRepository.findAll()).thenReturn(List.of(visitType));

        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.NOON, Duration.ofHours(1));
        PlannedVisit matchingVisit = new PlannedVisit("visit-1", LocalDate.of(2024, 1, 10), slot, "TYPE1", true, List.of(), VisitStatus.PROPOSED, List.of());
        PlannedVisit otherVisit = new PlannedVisit("visit-2", LocalDate.of(2024, 1, 12), slot, "TYPE1", true, List.of(), VisitStatus.CONFIRMED, List.of());

        MonthlyVisitPlan nullVisitPlan = new MonthlyVisitPlan(YearMonth.of(2024, 1));
        nullVisitPlan.setPlannedVisits(Arrays.asList((PlannedVisit) null));
        MonthlyVisitPlan matchingPlan = new MonthlyVisitPlan(YearMonth.of(2024, 1));
        matchingPlan.setPlannedVisits(List.of(matchingVisit));
        MonthlyVisitPlan otherPlan = new MonthlyVisitPlan(YearMonth.of(2024, 1));
        otherPlan.setPlannedVisits(List.of(otherVisit));

        when(monthlyVisitPlanRepository.findAll()).thenReturn(Arrays.asList(null, nullVisitPlan, matchingPlan, otherPlan));

        List<VisitOccurrenceDTO> occurrences = service.listVisitsByStatus(VisitStatus.PROPOSED);

        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.get(0).getId()).isEqualTo("visit-1");
    }

    @Test
    void listBookingsIgnoresNullPlansAndVisits() {
        VisitType visitType = mock(VisitType.class);
        when(visitType.getVisitTitle()).thenReturn("Title");
        when(visitTypeRepository.findById("TYPE1")).thenReturn(Optional.of(visitType));

        VisitBooking booking = new VisitBooking("code-1", "beneficiary", "Beneficiary", 2, "note");
        TimeSlot slot = new TimeSlot(DayOfWeek.TUESDAY, LocalTime.NOON, Duration.ofHours(1));
        PlannedVisit visitWithBooking = new PlannedVisit("visit-3", LocalDate.of(2024, 1, 15), slot, "TYPE1", true, List.of(), VisitStatus.CONFIRMED, List.of(booking));
        MonthlyVisitPlan planWithBooking = new MonthlyVisitPlan(YearMonth.of(2024, 1));
        planWithBooking.setPlannedVisits(List.of(visitWithBooking));

        MonthlyVisitPlan emptyPlan = new MonthlyVisitPlan(YearMonth.of(2024, 1));
        emptyPlan.setPlannedVisits(List.of());

        when(beneficiaryRepository.findByUsername("beneficiary")).thenReturn(Optional.of(mock(Beneficiary.class)));
        when(monthlyVisitPlanRepository.findAll()).thenReturn(Arrays.asList(null, emptyPlan, planWithBooking));

        List<VisitBookingDTO> bookings = service.listBookings(" beneficiary ");

        assertThat(bookings).hasSize(1);
        VisitBookingDTO dto = bookings.get(0);
        assertThat(dto.getCode()).isEqualTo("code-1");
        assertThat(dto.getBeneficiaryName()).isEqualTo("Beneficiary");
    }

    @Test
    void cancelBookingRemovesBookingEvenWithNullPlans() {
        when(beneficiaryRepository.findByUsername("beneficiary")).thenReturn(Optional.of(mock(Beneficiary.class)));

        VisitType visitType = mock(VisitType.class);
        when(visitTypeRepository.findById("TYPE1")).thenReturn(Optional.of(visitType));
        when(visitType.getMaxParticipants()).thenReturn(5);
        when(visitType.getMinParticipants()).thenReturn(2);

        VisitBooking booking = new VisitBooking("code-123", "beneficiary", "Beneficiary", 2, "note");
        TimeSlot slot = new TimeSlot(DayOfWeek.THURSDAY, LocalTime.NOON, Duration.ofHours(1));
        PlannedVisit visit = new PlannedVisit("visit-9", LocalDate.now().plusDays(3), slot, "TYPE1", true, List.of(), VisitStatus.CONFIRMED, List.of(booking));
        MonthlyVisitPlan plan = new MonthlyVisitPlan(YearMonth.now());
        plan.setPlannedVisits(List.of(visit));

        when(monthlyVisitPlanRepository.findAll()).thenReturn(Arrays.asList(null, plan));

        service.cancelBooking(" beneficiary ", "code-123", LocalDate.now());

        PlannedVisit updatedVisit = plan.getPlannedVisits().get(0);
        assertThat(updatedVisit.getBookings()).isEmpty();
        assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.PROPOSED);
        verify(monthlyVisitPlanRepository).save(plan);
    }
}
