package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.SystemSettings;
import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.SettingsRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceImpTest {

    @Mock
    private VolunteerRepository volunteerRepository;

    @Mock
    private SettingsRepository settingsRepository;

    private VolunteerServiceImp service;

    @BeforeEach
    void setUp() {
        service = new VolunteerServiceImp (volunteerRepository, settingsRepository);
    }

    @Nested
    @DisplayName("First access credentials")
    class FirstAccess {

        @Test
        void setPersonalCredentialsUpdatesNicknameAndPasswordAfterValidation() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));

            service.verifyDefaultCredentials ("vol001", "tempPass");
            service.setPersonalCredentials ("vol001", " guide.one ", " newSecret ");

            ArgumentCaptor<Volunteer> captor = ArgumentCaptor.forClass (Volunteer.class);
            verify (volunteerRepository).deleteByNickname ("vol001");
            verify (volunteerRepository).save (captor.capture ());

            Volunteer persisted = captor.getValue ();
            assertThat (persisted.getNickname ()).isEqualTo ("guide.one");
            assertThat (persisted.passwordMatches ("newSecret")).isTrue ();
            assertThat (persisted.isFirstAccessPending ()).isFalse ();
        }

        @Test
        void setPersonalCredentialsFailsIfNicknameUnchanged() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));

            service.verifyDefaultCredentials ("vol001", "tempPass");

            assertThatThrownBy (() -> service.setPersonalCredentials ("vol001", "vol001", "newSecret"))
                    .isInstanceOf (IllegalArgumentException.class)
                    .hasMessageContaining ("nickname");

            verify (volunteerRepository, never ()).save (any ());
        }

        @Test
        void setPersonalCredentialsFailsIfPasswordUnchanged() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));

            service.verifyDefaultCredentials ("vol001", "tempPass");

            assertThatThrownBy (() -> service.setPersonalCredentials ("vol001", "guide.one", "tempPass"))
                    .isInstanceOf (IllegalArgumentException.class)
                    .hasMessageContaining ("password");

            verify (volunteerRepository, never ()).deleteByNickname (any ());
        }
    }

    @Nested
    @DisplayName("Availability submission")
    class AvailabilitySubmission {

        @Test
        void submitAvailabilityRejectsDaysConflictingWithExcludedDates() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            volunteer.setPersonalCredentials ("Secret123!");
            YearMonth month = YearMonth.of (2024, 6);
            MonthlyAvailability availability = new MonthlyAvailability (
                    month,
                    EnumSet.of (DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    2,
                    LocalDate.of (2024, 5, 10)
            );
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));
            SystemSettings settings = new SystemSettings (
                    "scope",
                    10,
                    List.of (LocalDate.of (2024, 6, 10))
            );
            when (settingsRepository.load ()).thenReturn (Optional.of (settings));

            assertThatThrownBy (() -> service.submitAvailability ("vol001", availability, LocalDate.of (2024, 5, 20)))
                    .isInstanceOf (IllegalArgumentException.class)
                    .hasMessageContaining ("2024-06-10");

            verify (volunteerRepository, never ()).save (any ());
        }

        @Test
        void submitAvailabilityPersistsWhenNoConflictsFound() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            volunteer.setPersonalCredentials ("Secret123!");
            YearMonth month = YearMonth.of (2024, 6);
            MonthlyAvailability availability = new MonthlyAvailability (
                    month,
                    EnumSet.of (DayOfWeek.THURSDAY),
                    1,
                    LocalDate.of (2024, 5, 10)
            );
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));
            SystemSettings settings = new SystemSettings (
                    "scope",
                    10,
                    List.of (LocalDate.of (2024, 6, 10))
            );
            when (settingsRepository.load ()).thenReturn (Optional.of (settings));

            service.submitAvailability ("vol001", availability, LocalDate.of (2024, 5, 12));

            assertThat (volunteer.findAvailability (month)).isPresent ();
            verify (volunteerRepository).save (volunteer);
        }

        @Test
        void submitAvailabilityFailsWhenWindowClosed() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            volunteer.setPersonalCredentials ("Secret123!");
            YearMonth month = YearMonth.of (2024, 11);
            MonthlyAvailability availability = new MonthlyAvailability (
                    month,
                    EnumSet.of (DayOfWeek.MONDAY),
                    1,
                    LocalDate.of (2024, 10, 10)
            );
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));

            assertThatThrownBy (() -> service.submitAvailability ("vol001", availability, LocalDate.of (2024, 10, 23)))
                    .isInstanceOf (IllegalStateException.class)
                    .hasMessageContaining ("La finestra di caricamento è chiusa");

            verify (volunteerRepository, never ()).save (any ());
        }

    }
}