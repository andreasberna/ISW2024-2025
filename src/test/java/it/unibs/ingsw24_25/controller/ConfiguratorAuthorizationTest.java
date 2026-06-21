package it.unibs.ingsw24_25.controller;

import it.unibs.ingsw24_25.repository.MonthlyVisitPlanRepository;
import it.unibs.ingsw24_25.repository.PlannedVisitRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import it.unibs.ingsw24_25.service.ConfiguratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfiguratorController.class)
class ConfiguratorAuthorizationTest {

    /** Enables @PreAuthorize processing in the test slice. */
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig {}

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ConfiguratorService configuratorService;

    @MockBean
    MonthlyVisitPlanRepository monthlyVisitPlanRepository;

    @MockBean
    PlannedVisitRepository plannedVisitRepository;

    @MockBean
    VolunteerRepository volunteerRepository;

    @Test
    @WithMockUser(username = "volunteer1", roles = "VOLUNTEER")
    void initPlan_whenRoleIsVolunteer_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/configurator/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMonth\":\"2027-03\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "volunteer1", roles = "VOLUNTEER")
    void addPlace_whenRoleIsVolunteer_shouldReturn403() throws Exception {
        // Use correct field names from CreatePlaceRequest DTO (title + location)
        mockMvc.perform(post("/api/configurator/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Museo Civico\",\"location\":\"Via Roma 1\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "configurator1", roles = "CONFIGURATOR")
    void initPlan_whenRoleIsConfigurator_shouldNotReturn403() throws Exception {
        // The request may fail with 400 (validation) or 201, but must not be 403
        mockMvc.perform(post("/api/configurator/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetMonth\":\"2027-03\"}")
                        .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }
}