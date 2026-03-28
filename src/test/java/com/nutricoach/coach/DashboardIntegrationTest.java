package com.nutricoach.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanDayRepository;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.repository.ProgressLogRepository;
import com.nutricoach.progress.repository.ProgressPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired MealPlanDayRepository mealPlanDayRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired ProgressPhotoRepository progressPhotoRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9700000001").ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                // Delete progress photos → logs
                progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId())
                        .forEach(l -> progressPhotoRepository.deleteAll(
                                progressPhotoRepository.findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(existing.getId(), l.getId())));
                progressLogRepository.deleteAll(
                        progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId()));
                // Delete meal plan days → plans
                mealPlanRepository.findAll().stream()
                        .filter(p -> p.getCoachId().equals(existing.getId()) && p.getClientId().equals(c.getId()))
                        .forEach(plan -> {
                            mealPlanDayRepository.deleteAll(
                                    mealPlanDayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()));
                            mealPlanRepository.delete(plan);
                        });
            });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9700000001")
                .name("Dashboard Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void dashboard_noClients_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/v1/coach/dashboard")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalClients").value(0))
                .andExpect(jsonPath("$.data.totalMealPlans").value(0))
                .andExpect(jsonPath("$.data.clientsNeedingPlan").value(0))
                .andExpect(jsonPath("$.data.recentClients").isArray());
    }

    @Test
    void dashboard_withClients_returnsCorrectCounts() throws Exception {
        // 2 ACTIVE, 1 ONBOARDING
        saveClient("9700000002", "Alice", Client.Status.ACTIVE);
        saveClient("9700000003", "Bob",   Client.Status.ACTIVE);
        saveClient("9700000004", "Carol", Client.Status.ONBOARDING);

        mockMvc.perform(get("/api/v1/coach/dashboard")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(3))
                .andExpect(jsonPath("$.data.activeClients").value(2))
                .andExpect(jsonPath("$.data.onboardingClients").value(1))
                .andExpect(jsonPath("$.data.inactiveClients").value(0))
                .andExpect(jsonPath("$.data.clientsNeedingPlan").value(3));
    }

    @Test
    void dashboard_clientsNeedingPlan_excludesClientWithPlan() throws Exception {
        Client withPlan    = saveClient("9700000005", "Has Plan",  Client.Status.ACTIVE);
        Client withoutPlan = saveClient("9700000006", "No Plan",   Client.Status.ACTIVE);

        mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId())
                .clientId(withPlan.getId())
                .name("Week 1 Plan")
                .status(MealPlan.Status.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/coach/dashboard")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(2))
                .andExpect(jsonPath("$.data.totalMealPlans").value(1))
                .andExpect(jsonPath("$.data.clientsNeedingPlan").value(1));
    }

    @Test
    void dashboard_recentClients_returnsLatestFive() throws Exception {
        for (int i = 1; i <= 7; i++) {
            saveClient("970000010" + i, "Client " + i, Client.Status.ACTIVE);
        }

        mockMvc.perform(get("/api/v1/coach/dashboard")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClients").value(7))
                .andExpect(jsonPath("$.data.recentClients.length()").value(5));
    }

    @Test
    void dashboard_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/coach/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    private Client saveClient(String phone, String name, Client.Status status) {
        return clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone(phone)
                .name(name)
                .status(status)
                .build());
    }
}
