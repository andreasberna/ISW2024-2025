package it.unibs.ingsw24_25.util;

import it.unibs.ingsw24_25.model.Volunteer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JSONSupportVolunteerTest {

    @Test
    void serializeAndDeserializeVolunteerPreservesPassword() {
        Volunteer volunteer = new Volunteer ("alice", "password");
        Map<String, Volunteer> volunteers = new HashMap<> ();
        volunteers.put(volunteer.getNickname(), volunteer);

        String json = JSONSupport.serializeVolunteerMap(volunteers);
        Map<String, Volunteer> restored = JSONSupport.deserializeVolunteerMap(json);

        Volunteer reloaded = restored.get("alice");
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getPassword()).isEqualTo("password");
    }
}
