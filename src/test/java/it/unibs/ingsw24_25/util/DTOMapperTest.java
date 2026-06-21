package it.unibs.ingsw24_25.util;

import it.unibs.ingsw24_25.DTO.response.PlaceResponse;
import it.unibs.ingsw24_25.DTO.response.VolunteerResponse;
import it.unibs.ingsw24_25.model.Place;
import it.unibs.ingsw24_25.model.Volunteer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DTOMapperTest {

    @Test
    void placeToDtoCopiesBasicFields() {
        Place place = new Place("Museo", "descr", "Brescia");

        PlaceResponse dto = DTOMapper.placeToDTO(place);

        assertThat(dto.placeTitle()).isEqualTo("Museo");
        assertThat(dto.placeDescription()).isEqualTo("descr");
        assertThat(dto.location()).isEqualTo("Brescia");
    }

    @Test
    void volunteerToDtoListsLinkedVisits() {
        Volunteer volunteer = new Volunteer("alice", "password");

        VolunteerResponse dto = DTOMapper.volunteerToDTO(volunteer);

        assertThat(dto.nickname()).isEqualTo("alice");
        assertThat(dto.active()).isTrue();
        assertThat(dto.firstAccessPending()).isTrue();
    }
}