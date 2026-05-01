package com.nutricoach.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkoutAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired ClientWorkoutAssignmentRepository assignmentRepository;
    @Autowired ClientWorkoutScheduleRepository scheduleRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Coach otherCoach;
    private Client client1;
    private Client client2;
    private Client otherClient;
    private Workout workout;

    @BeforeEach
    void setup() {
        cleanup("9800040001");
        cleanup("9800040002");

        coach = coachRepository.save(Coach.builder()
                .phone("9800040001").name("Assignment Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        otherCoach = coachRepository.save(Coach.builder()
                .phone("9800040002").name("Other Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client1 = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Client A").phone("9111111111")
                .status(Client.Status.ACTIVE).build());
        client2 = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Client B").phone("9111111112")
                .status(Client.Status.ACTIVE).build());
        otherClient = clientRepository.save(Client.builder()
                .coachId(otherCoach.getId()).name("Other's Client").phone("9111111113")
                .status(Client.Status.ACTIVE).build());

        workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name("Test Workout").build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            scheduleRepository.findAll().stream()
                    .filter(s -> s.getCoachId().equals(existing.getId()))
                    .forEach(scheduleRepository::delete);
            assignmentRepository.findAll().stream()
                    .filter(a -> a.getCoachId().equals(existing.getId()))
                    .forEach(assignmentRepository::delete);
            workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(existing.getId()))
                    .forEach(workoutRepository::delete);
            clientRepository.findAll().stream()
                    .filter(c -> c.getCoachId().equals(existing.getId()))
                    .forEach(clientRepository::delete);
            coachRepository.delete(existing);
        });
    }

    @Test
    void assign_multipleClients_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(client1.getId(), client2.getId()),
                                "notes", "Start Monday"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void assign_idempotent_returnsExistingForDuplicate() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(client1.getId())))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(client1.getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void assign_crossTenantClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(otherClient.getId())))))
                .andExpect(status().isNotFound());
    }

    @Test
    void assign_unknownWorkout_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(client1.getId())))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unassign_removesFromList() throws Exception {
        String response = mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(client1.getId())))))
                .andReturn().getResponse().getContentAsString();
        String assignmentId = objectMapper.readTree(response).path("data").get(0).path("id").asText();

        mockMvc.perform(delete("/api/v1/library/workouts/{wid}/assignments/{id}", workout.getId(), assignmentId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void schedule_createsEntry_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/schedules", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientId", client1.getId().toString(),
                                "date", "2026-05-15",
                                "notes", "Morning session"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.date").value("2026-05-15"));
    }

    @Test
    void schedule_crossTenantClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/schedules", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientId", otherClient.getId().toString(),
                                "date", "2026-05-15"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void listSchedules_filtersDateRange() throws Exception {
        for (String d : List.of("2026-05-01", "2026-05-15", "2026-06-10")) {
            mockMvc.perform(post("/api/v1/library/workouts/{id}/schedules", workout.getId())
                            .header("Authorization", "Bearer " + jwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "clientId", client1.getId().toString(),
                                    "date", d))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/library/clients/{id}/schedules", client1.getId())
                        .param("from", "2026-05-01").param("to", "2026-05-31")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void unschedule_removesEntry() throws Exception {
        String response = mockMvc.perform(post("/api/v1/library/workouts/{id}/schedules", workout.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientId", client1.getId().toString(),
                                "date", "2026-05-20"))))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        String scheduleId = data.path("id").asText();

        mockMvc.perform(delete("/api/v1/library/schedules/{id}", scheduleId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/clients/{id}/schedules", client1.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void assign_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/assignments", workout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "clientIds", List.of(client1.getId())))))
                .andExpect(status().isUnauthorized());
    }
}
