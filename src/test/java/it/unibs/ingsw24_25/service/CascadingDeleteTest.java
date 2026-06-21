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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CascadingDeleteTest {

    @Mock PlaceRepository placeRepository;
    @Mock VisitTypeRepository visitTypeRepository;
    @Mock VolunteerRepository volunteerRepository;
    @Mock SettingsRepository settingsRepository;
    @Mock MonthlyVisitPlanRepository monthlyVisitPlanRepository;
    @Mock PlannedVisitRepository plannedVisitRepository;
    @Mock ConfiguratorCredentialManager credentialManager;

    @InjectMocks
    private ConfiguratorServiceImp service;

    @Test
    void removeVisitType_shouldRemoveVolunteerAssociationAndDeleteEntity() {
        Place place = new Place("Museo", "Desc", "Via Roma");
        VisitType vtToDelete = new VisitType("Visita A", "Desc", "Ingresso",
                LocalDate.now(), LocalDate.now().plusYears(1),
                List.of(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), 60)),
                false, 1, 10, place);
        VisitType vtRemaining = new VisitType("Visita B", "Desc", "Ingresso",
                LocalDate.now(), LocalDate.now().plusYears(1),
                List.of(new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(14, 0), 60)),
                false, 1, 10, place);
        // JPA doesn't run in unit tests: set IDs manually
        ReflectionTestUtils.setField(vtToDelete, "id", "vt-a-id");
        ReflectionTestUtils.setField(vtRemaining, "id", "vt-b-id");

        // Place has both visit types
        place.setVisits(List.of(vtToDelete, vtRemaining));

        // Volunteer qualifies for both visit types
        Volunteer volunteer = new Volunteer("guide1", "pass");
        volunteer.addVisit(vtToDelete);
        volunteer.addVisit(vtRemaining);

        when(visitTypeRepository.findById("vt-a-id")).thenReturn(Optional.of(vtToDelete));
        when(volunteerRepository.findAll()).thenReturn(List.of(volunteer));

        service.removeVisitType("vt-a-id");

        // Volunteer still has one visit type remaining → saved (not deleted)
        assertThat(volunteer.getVisitsAttending()).doesNotContain(vtToDelete);
        assertThat(volunteer.getVisitsAttending()).contains(vtRemaining);
        verify(volunteerRepository).save(volunteer);

        // Place still has one visit type → saved (not deleted)
        verify(placeRepository).save(place);
        verify(placeRepository, never()).delete(any());

        // Visit type itself is deleted
        verify(visitTypeRepository).delete(vtToDelete);
    }

    @Test
    void removeVisitType_whenVolunteerHasOnlyThisVisit_shouldDeleteVolunteer() {
        Place place = new Place("Parco", "Desc", "Via Parco");
        VisitType onlyVt = new VisitType("Visita Sola", "Desc", "Cancello",
                LocalDate.now(), LocalDate.now().plusYears(1),
                List.of(new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), 90)),
                false, 2, 8, place);
        // JPA doesn't run in unit tests: set ID manually
        ReflectionTestUtils.setField(onlyVt, "id", "vt-solo");
        place.setVisits(List.of(onlyVt));

        Volunteer volunteer = new Volunteer("guide2", "pass");
        volunteer.addVisit(onlyVt);

        when(visitTypeRepository.findById("vt-solo")).thenReturn(Optional.of(onlyVt));
        when(volunteerRepository.findAll()).thenReturn(List.of(volunteer));
        // removeVolunteer internally calls findByNickname and findByVolunteer
        when(volunteerRepository.findByNickname("guide2")).thenReturn(Optional.of(volunteer));
        when(plannedVisitRepository.findByVolunteer(volunteer)).thenReturn(List.of());

        service.removeVisitType("vt-solo");

        // Volunteer with empty visitsAttending list → deleted via removeVolunteer()
        verify(volunteerRepository).delete(volunteer);
        verify(visitTypeRepository).delete(onlyVt);
    }
}