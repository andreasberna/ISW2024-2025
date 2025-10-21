package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.Volunteer;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceImpTest {

    @Mock
    private VolunteerRepository volunteerRepository;

    private VolunteerServiceImp service;

    @BeforeEach
    void setUp() {
        service = new VolunteerServiceImp (volunteerRepository);
    }

    @Nested
    @DisplayName("First access credentials")
    class FirstAccess {

        @Test
        void setPersonalCredentialsUpdatesNicknameAndPasswordAfterValidation() {
            Volunteer volunteer = new Volunteer ("vol001", "tempPass");
            when (volunteerRepository.findByNickname ("vol001")).thenReturn (Optional.of (volunteer));
            when (volunteerRepository.findByNickname ("guide.one")).thenReturn (Optional.empty ());

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
            when (volunteerRepository.findByNickname ("guide.one")).thenReturn (Optional.empty ());

            service.verifyDefaultCredentials ("vol001", "tempPass");

            assertThatThrownBy (() -> service.setPersonalCredentials ("vol001", "guide.one", "tempPass"))
                    .isInstanceOf (IllegalArgumentException.class)
                    .hasMessageContaining ("password");

            verify (volunteerRepository, never ()).deleteByNickname (any ());
        }
    }
}