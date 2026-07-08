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
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.entity.ProgressLog;
import com.nutricoach.progress.repository.CheckInRepository;
import com.nutricoach.progress.repository.ProgressLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the client-portal write endpoints:
 *   POST /api/v1/portal/check-ins  and  POST /api/v1/portal/progress.
 * Identity is taken from a CLIENT-role JWT, never from the request body.
 */
class PortalProgressWriteIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired CheckInRepository checkInRepository;
    @Autowired JwtService jwtService;

    private Coach coach;
    private Client client;
    private MealPlan mealPlan;
    private String clientJwt;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9600000101").ifPresent(existing -> {
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
                .phone("9600000101")
                .name("Portal Progress Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9600000102")
                .name("Portal Progress Client")
                .status(Client.Status.ACTIVE)
                .build());

        mealPlan = mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId())
                .clientId(client.getId())
                .name("Portal Test Plan")
                .status(MealPlan.Status.ACTIVE)
                .build());

        clientJwt = jwtService.generateClientToken(client.getPhone(), client.getId(), coach.getId());
    }

    // ── Check-in submission ────────────────────────────────────────────────────

    @Test
    void submitCheckIn_validRequest_returns201AndPersistsUnderTokenScope() throws Exception {
        mockMvc.perform(post("/api/v1/portal/check-ins")
                        .header("Authorization", "Bearer " + clientJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString(),
                                "mealPlanId", mealPlan.getId().toString(),
                                "adherencePercent", 80,
                                "clientNotes", "Great week"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adherencePercent").value(80))
                .andExpect(jsonPath("$.data.mealPlanId").value(mealPlan.getId().toString()))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()));

        // A follow-up GET on the same portal controller returns the created row.
        mockMvc.perform(get("/api/v1/portal/check-ins")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].clientId").value(client.getId().toString()));

        // The persisted row is scoped to the token's coachId/clientId — not any body value.
        List<CheckIn> rows =
                checkInRepository.findByClientIdAndCoachIdOrderByCheckInDateDesc(client.getId(), coach.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCoachId()).isEqualTo(coach.getId());
        assertThat(rows.get(0).getClientId()).isEqualTo(client.getId());
    }

    @Test
    void submitCheckIn_missingMealPlanId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/portal/check-ins")
                        .header("Authorization", "Bearer " + clientJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submitCheckIn_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/portal/check-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "checkInDate", LocalDate.now().toString(),
                                "mealPlanId", mealPlan.getId().toString()
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── Progress logging ───────────────────────────────────────────────────────

    @Test
    void logProgress_validRequest_returns201AndPersistsUnderTokenScope() throws Exception {
        mockMvc.perform(post("/api/v1/portal/progress")
                        .header("Authorization", "Bearer " + clientJwt)
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

        // A follow-up GET on the same portal controller returns the created row.
        mockMvc.perform(get("/api/v1/portal/progress")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].weightKg").value(72.5));

        // The persisted row is scoped to the token's coachId/clientId.
        List<ProgressLog> rows =
                progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(client.getId(), coach.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCoachId()).isEqualTo(coach.getId());
        assertThat(rows.get(0).getClientId()).isEqualTo(client.getId());
    }

    @Test
    void logProgress_missingDate_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/portal/progress")
                        .header("Authorization", "Bearer " + clientJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("weightKg", 72.5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logProgress_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/portal/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loggedDate", LocalDate.now().toString()
                        ))))
                .andExpect(status().isUnauthorized());
    }
}
