package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceImpTest {

    @Mock
    private VolunteerRepository volunteerRepository;

    @InjectMocks
    private VolunteerServiceImp service;

    @Nested
    @DisplayName("Availability submission")
    class AvailabilitySubmission {

        @Test
        void submitAvailabilityAcceptsQualifiedDays() {
            Volunteer volunteer = new Volunteer("vol001", "Secret123!");
            VisitType visitType = new VisitType("V1", "Desc", "Meet", LocalDate.now(), null, List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), 60)), false, 1, 1, null);
            volunteer.addVisit(visitType);

            when(volunteerRepository.findByNickname("vol001")).thenReturn(Optional.of(volunteer));

            MonthlyAvailability availability = new MonthlyAvailability(
                    YearMonth.now().plusMonths(1),
                    EnumSet.of(DayOfWeek.MONDAY),
                    1,
                    LocalDate.now(),
                    false,
                    null
            );

            // Use a date within the submission window (day 5 of the month before target)
            LocalDate withinWindow = availability.getMonth().minusMonths(1).atDay(5);
            service.submitAvailability("vol001", availability, withinWindow);

            verify(volunteerRepository).save(volunteer);
        }
    }
}
