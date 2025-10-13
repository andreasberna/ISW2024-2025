package it.unibs.ingsw24_25.util;

import it.unibs.ingsw24_25.DTO.PlaceDTO;
import it.unibs.ingsw24_25.DTO.VisitTypeDTO;
import it.unibs.ingsw24_25.DTO.VolunteerDTO;
import it.unibs.ingsw24_25.model.Place;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.model.VisitType;
import it.unibs.ingsw24_25.model.Volunteer;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DTOMapperTest {

    @Test
    void placeToDtoCopiesBasicFields() {
        Place place = new Place("Museo", "descr", "Brescia");

        PlaceDTO dto = DTOMapper.placeToDTO(place);

        assertThat(dto.getTitle()).isEqualTo("Museo");
        assertThat(dto.getDescription()).isEqualTo("descr");
        assertThat(dto.getLocation()).isEqualTo("Brescia");
    }

    @Test
    void visitTypeToDtoFormatsScheduleAndParticipants() {
        Place place = new Place("Museo", "descr", "Brescia");
        TimeSlot slot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 30), Duration.ofMinutes(90));
        VisitType visitType = mock(VisitType.class);
        when(visitType.getVisitTitle()).thenReturn("Visita");
        when(visitType.getSchedules()).thenReturn(List.of(slot));
        when(visitType.getTicketRequired()).thenReturn(true);
        when(visitType.getMinParticipants()).thenReturn(5);
        when(visitType.getMaxParticipants()).thenReturn(15);
        when(visitType.getPlace()).thenReturn(place);
        when(visitType.getState()).thenReturn(null);

        VisitTypeDTO dto = DTOMapper.visitTypeToDTO(visitType);

        assertThat(dto.getTitle()).isEqualTo("Visita");
        assertThat(dto.getDaySummary()).contains("Lun");
        assertThat(dto.getStartTime()).isEqualTo("09:30");
        assertThat(dto.getendTime()).isEqualTo("11:00");
        assertThat(dto.getDurationMinutes()).isEqualTo(90);
        assertThat(dto.getPlaceID()).isEqualTo("Museo");
        assertThat(dto.getState()).isNull();
    }

    @Test
    void volunteerToDtoListsLinkedVisits() {
        Volunteer volunteer = new Volunteer("alice");
        VisitType visitType = mock(VisitType.class);
        when(visitType.getVisitTitle()).thenReturn("Visita");
        volunteer.addVisit(visitType);

        VolunteerDTO dto = DTOMapper.volunteerToDTO(volunteer);

        assertThat(dto.getNickname()).isEqualTo("alice");
        assertThat(dto.getVisitTypeTitles()).containsExactly("Visita");
    }
}