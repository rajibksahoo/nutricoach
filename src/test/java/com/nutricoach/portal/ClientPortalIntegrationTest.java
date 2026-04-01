package com.nutricoach.portal;

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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientPortalIntegrationTest extends AbstractIntegrationTest {

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
    private Client otherClient;
    private MealPlan mealPlan;
    private ProgressLog progressLog;
    private CheckIn checkIn;

    private String clientJwt;
    private String coachJwt;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9500000001").ifPresent(existing -> {
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
                .phone("9500000001")
                .name("Portal Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9500000002")
                .name("Portal Client")
                .status(Client.Status.ACTIVE)
                .build());

        otherClient = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9500000003")
                .name("Other Client")
                .status(Client.Status.ACTIVE)
                .build());

        mealPlan = mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId())
                .clientId(client.getId())
                .name("Weight Loss Week 1")
                .build());

        progressLog = progressLogRepository.save(ProgressLog.builder()
                .clientId(client.getId())
                .coachId(coach.getId())
                .loggedDate(LocalDate.now())
                .weightKg(new java.math.BigDecimal("72.5"))
                .adherencePercent(85)
                .build());

        checkIn = checkInRepository.save(CheckIn.builder()
                .clientId(client.getId())
                .coachId(coach.getId())
                .mealPlanId(mealPlan.getId())
                .checkInDate(LocalDate.now())
                .adherencePercent(90)
                .build());

        clientJwt = jwtService.generateClientToken(client.getPhone(), client.getId(), coach.getId());
        coachJwt  = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    // ── /api/v1/portal/profile ────────────────────────────────────────────────

    @Test
    void profile_clientToken_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/portal/profile")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Portal Client"))
                .andExpect(jsonPath("$.data.phone").value("9500000002"));
    }

    @Test
    void profile_coachToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal/profile")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void profile_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/portal/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ── /api/v1/portal/meal-plans ─────────────────────────────────────────────

    @Test
    void mealPlans_clientToken_returnsOwnPlans() throws Exception {
        mockMvc.perform(get("/api/v1/portal/meal-plans")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(mealPlan.getId().toString()))
                .andExpect(jsonPath("$.data[0].name").value("Weight Loss Week 1"));
    }

    @Test
    void mealPlanDetail_clientToken_returnsPlan() throws Exception {
        mockMvc.perform(get("/api/v1/portal/meal-plans/" + mealPlan.getId())
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(mealPlan.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Weight Loss Week 1"));
    }

    @Test
    void mealPlanDetail_otherClientsPlan_returns404() throws Exception {
        // planId belongs to 'client', not 'otherClient' — must be rejected
        String otherClientJwt = jwtService.generateClientToken(
                otherClient.getPhone(), otherClient.getId(), coach.getId());

        mockMvc.perform(get("/api/v1/portal/meal-plans/" + mealPlan.getId())
                        .header("Authorization", "Bearer " + otherClientJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void mealPlanDetail_nonExistentPlan_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/portal/meal-plans/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void mealPlans_coachToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal/meal-plans")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isForbidden());
    }

    // ── /api/v1/portal/progress ───────────────────────────────────────────────

    @Test
    void progress_clientToken_returnsOwnHistory() throws Exception {
        mockMvc.perform(get("/api/v1/portal/progress")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].weightKg").value(72.5));
    }

    @Test
    void progressChart_clientToken_returnsChartData() throws Exception {
        mockMvc.perform(get("/api/v1/portal/progress/chart?days=30")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void progress_coachToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal/progress")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isForbidden());
    }

    // ── /api/v1/portal/check-ins ──────────────────────────────────────────────

    @Test
    void checkIns_clientToken_returnsOwnHistory() throws Exception {
        mockMvc.perform(get("/api/v1/portal/check-ins")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].adherencePercent").value(90));
    }

    @Test
    void checkIns_coachToken_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/portal/check-ins")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isForbidden());
    }
}
