package it.unibs.ingsw24_25.service;

import it.unibs.ingsw24_25.model.PlanningPhase;
import it.unibs.ingsw24_25.model.SystemSettings;
import it.unibs.ingsw24_25.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerritoryImmutabilityTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private VisitTypeRepository visitTypeRepository;
    @Mock
    private VolunteerRepository volunteerRepository;
    @Mock
    private SettingsRepository settingsRepository;
    @Mock
    private MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    @Mock
    private PlannedVisitRepository plannedVisitRepository;
    @Mock
    private ConfiguratorCredentialManager credentialManager;

    @InjectMocks
    private ConfiguratorServiceImp service;

    @Test
    void defineTerritorialScope_whenAlreadySet_shouldThrowIllegalState() {
        SystemSettings existing = new SystemSettings(1L, "Milano", 15, List.of(), null,
                PlanningPhase.RACCOLTA_DISPONIBILITA, null);
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.defineTerritorialScope("Roma"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already defined");
    }

    @Test
    void defineTerritorialScope_whenNotSet_shouldPersist() {
        SystemSettings empty = new SystemSettings(1L, null, 15, List.of(), null,
                PlanningPhase.RACCOLTA_DISPONIBILITA, null);
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(empty));

        service.defineTerritorialScope("Brescia");

        org.mockito.Mockito.verify(settingsRepository).save(empty);
    }
}