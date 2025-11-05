package it.unibs.ingsw24_25.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceTest {

    @Test
    void constructorRequiresNonBlankTitle() {
        assertThatThrownBy(() -> new Place(null, "desc", "location"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titolo");
        assertThatThrownBy(() -> new Place("   ", "desc", "location"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titolo");
    }

    @Test
    void settersTrimValues() {
        Place place = new Place("Museo", null, "  centro " );

        place.setPlaceDescription("  descrizione ");
        place.setLocation("  location ");

        assertThat(place.getPlaceDescription()).isEqualTo("descrizione");
        assertThat(place.getLocation()).isEqualTo("location");
    }
}
