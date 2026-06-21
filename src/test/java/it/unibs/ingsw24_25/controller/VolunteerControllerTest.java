package it.unibs.ingsw24_25.controller;

import it.unibs.ingsw24_25.DTO.AssignedShiftDTO;
import it.unibs.ingsw24_25.model.MonthlyAvailability;
import it.unibs.ingsw24_25.model.TimeSlot;
import it.unibs.ingsw24_25.repository.PlannedVisitRepository;
import it.unibs.ingsw24_25.repository.VolunteerRepository;
import it.unibs.ingsw24_25.service.VolunteerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VolunteerController.class)
class VolunteerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VolunteerService volunteerService;

    @MockBean
    private VolunteerRepository volunteerRepository;

    @MockBean
    private PlannedVisitRepository plannedVisitRepository;

    @Test
    @WithMockUser(username = "testuser", roles = "VOLUNTEER")
    void submitAvailability_shouldSucceed() throws Exception {
        doNothing().when(volunteerService).submitAvailability(eq("testuser"), any(MonthlyAvailability.class), any(LocalDate.class));

        String availabilityJson = "{\"month\":\"2099-01\",\"availableDays\":[\"MONDAY\"]}";

        mockMvc.perform(post("/api/volunteers/testuser/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "VOLUNTEER")
    void getAssignedVisits_shouldReturnListOfShifts() throws Exception {
        AssignedShiftDTO shift = new AssignedShiftDTO(YearMonth.of(2024, 8), LocalDate.of(2024, 8, 5), "Test Visit", new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), 60));
        when(volunteerService.getAssignedShifts("testuser")).thenReturn(List.of(shift));

        mockMvc.perform(get("/api/volunteers/assigned-visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitTypeId").value("Test Visit"));
    }
}