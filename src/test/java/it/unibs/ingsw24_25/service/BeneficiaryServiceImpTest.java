package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.DTO.VisitBookingDTO;
import it.unibs.ingsw24_25.DTO.VisitOccurrenceDTO;
import it.unibs.ingsw24_25.model.Beneficiary;
import it.unibs.ingsw24_25.model.VisitStatus;
import it.unibs.ingsw24_25.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceImpTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private BeneficiaryBookingManager bookingManager;
    @Mock
    private PasswordEncoder passwordEncoder;

    private BeneficiaryServiceImp service;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryServiceImp(
                beneficiaryRepository,
                bookingManager,
                passwordEncoder
        );
    }

    @Test
    void registerRejectsBlankUsername() {
        assertThatThrownBy(() -> service.register("Mario Rossi", "   ", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    void registerRejectsExistingUsername() {
        when(beneficiaryRepository.findByUsername("nickname")).thenReturn(Optional.of(new Beneficiary("nickname", "hash", "Mario Rossi")));

        assertThatThrownBy(() -> service.register("Mario Rossi", "nickname", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già utilizzato");
    }

    @Test
    void registerPersistsSanitizedBeneficiary() {
        when(beneficiaryRepository.findByUsername("nickname")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-secret");

        service.register("  Mario Rossi  ", "  nickname  ", "  secret  ");

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(beneficiaryRepository).save(captor.capture());
        Beneficiary saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("nickname");
        assertThat(saved.getFullName()).isEqualTo("Mario Rossi");
        assertThat(saved.getPassword()).isEqualTo("hashed-secret");
    }

    @Test
    void bookVisitDelegatesToManager() {
        LocalDate today = LocalDate.now();
        when(bookingManager.bookVisit("user", "visit-123", 2, "note", today)).thenReturn("code-123");

        String bookingCode = service.bookVisit("user", "visit-123", 2, "note", today);

        assertThat(bookingCode).isEqualTo("code-123");
        verify(bookingManager).bookVisit("user", "visit-123", 2, "note", today);
    }

    @Test
    void listVisitsByStatusDelegatesToManager() {
        VisitOccurrenceDTO dto = new VisitOccurrenceDTO("1", null, LocalDate.now(), null, "title", "desc", "loc", false, 1, 10, 2, VisitStatus.PROPOSTA, "type", List.of(), 1L, "title", "nickname");
        when(bookingManager.listVisitsByStatus(VisitStatus.PROPOSTA)).thenReturn(List.of(dto));

        List<VisitOccurrenceDTO> occurrences = service.listVisitsByStatus(VisitStatus.PROPOSTA);

        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.get(0).getId()).isEqualTo("1");
    }

    @Test
    void listBookingsDelegatesToManager() {
        VisitBookingDTO dto = new VisitBookingDTO("code", "name", 2, "note", LocalDate.now(), "title", VisitStatus.PROPOSTA);
        when(bookingManager.listBookings("user")).thenReturn(List.of(dto));

        List<VisitBookingDTO> bookings = service.listBookings("user");

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getCode()).isEqualTo("code");
    }

    @Test
    void cancelBookingDelegatesToManager() {
        LocalDate today = LocalDate.now();
        service.cancelBooking("user", "code-123", today);
        verify(bookingManager).cancelBooking("user", "code-123", today);
    }
}
