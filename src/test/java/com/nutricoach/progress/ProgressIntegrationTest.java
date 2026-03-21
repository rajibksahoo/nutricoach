package com.nutricoach.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.repository.CheckInRepository;
import com.nutricoach.progress.repository.ProgressLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProgressIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired CheckInRepository checkInRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private MealPlan mealPlan;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9600000001").ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                checkInRepository.deleteAll(
                        checkInRepository.findByClientIdAndCoachIdOrderByCheckInDateDesc(c.getId(), existing.getId()));
                progressLogRepository.deleteAll(
                        progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId()));
                mealPlanRepository.findByClientIdAndCoachIdAndDeletedAtIsNull(c.getId(), existing.getId())
                        .forEach(mealPlanRepository::delete);
            });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9600000001")
                .name("Progress Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9600000002")
                .name("Progress Client")
                .status(Client.Status.ACTIVE)
                .build());

        mealPlan = mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId())
                .clientId(client.getId())
                .name("Test Plan")
                .status(MealPlan.Status.ACTIVE)
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    // ── Progress Log Tests ────────────────────────────────────────────────────

    @Test
    void logProgress_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().toString(),
                                "weightKg", 72.5,
                                "adherencePercent", 85
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.weightKg").value(72.5))
                .andExpect(jsonPath("$.data.adherencePercent").value(85))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()));
    }

    @Test
    void logProgress_sameDateTwice_upserts() throws Exception {
        String today = LocalDate.now().toString();

        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", today,
                                "weightKg", 72.5
                        ))))
                .andExpect(status().isCreated());

        // Second log same date — should update, not 409
        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", today,
                                "weightKg", 73.0,
                                "adherencePercent", 90
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weightKg").value(73.0))
                .andExpect(jsonPath("$.data.adherencePercent").value(90));
    }

    @Test
    void logProgress_missingDate_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weightKg", 72.5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logProgress_wrongClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/progress", java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().toString()
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void logProgress_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().toString()
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getHistory_returnsLogsDescending() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().minusDays(1).toString(), "weightKg", 71.0))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().toString(), "weightKg", 72.0))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].weightKg").value(72.0)) // most recent first
                .andExpect(jsonPath("$.data[1].weightKg").value(71.0));
    }

    @Test
    void getChart_returnsAscendingWithinRange() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().minusDays(5).toString(), "weightKg", 70.0))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clients/{id}/progress", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().toString(), "weightKg", 69.5))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clients/{id}/progress/chart?days=30", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].weightKg").value(70.0)) // ascending
                .andExpect(jsonPath("$.data[1].weightKg").value(69.5));
    }

    // ── Check-in Tests ────────────────────────────────────────────────────────

    @Test
    void createCheckIn_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString(),
                                "mealPlanId", mealPlan.getId().toString(),
                                "adherencePercent", 80,
                                "clientNotes", "Felt great this week"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adherencePercent").value(80))
                .andExpect(jsonPath("$.data.mealPlanId").value(mealPlan.getId().toString()));
    }

    @Test
    void createCheckIn_duplicateDate_returns409() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "checkInDate", LocalDate.now().toString(),
                "mealPlanId", mealPlan.getId().toString()
        ));

        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createCheckIn_invalidMealPlan_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString(),
                                "mealPlanId", java.util.UUID.randomUUID().toString()
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCheckIn_missingMealPlanId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCheckInHistory_returnsDescending() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().minusDays(7).toString(),
                                "mealPlanId", mealPlan.getId().toString(),
                                "adherencePercent", 70
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString(),
                                "mealPlanId", mealPlan.getId().toString(),
                                "adherencePercent", 90
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clients/{id}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].adherencePercent").value(90)) // most recent first
                .andExpect(jsonPath("$.data[1].adherencePercent").value(70));
    }

    @Test
    void checkIns_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/check-ins", client.getId()))
                .andExpect(status().isUnauthorized());
    }
}
