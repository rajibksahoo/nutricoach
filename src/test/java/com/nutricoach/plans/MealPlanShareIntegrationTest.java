package com.nutricoach.plans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.notifications.repository.NotificationLogRepository;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanDayRepository;
import com.nutricoach.plans.repository.MealPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MealPlanShareIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired MealPlanDayRepository mealPlanDayRepository;
    @Autowired NotificationLogRepository notificationLogRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private MealPlan mealPlan;

    private static final String PHONE = "9800000001";

    @BeforeEach
    void setup() {
        coachRepository.findByPhone(PHONE).ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                notificationLogRepository.deleteAll(
                        notificationLogRepository.findByCoachIdAndClientIdOrderByCreatedAtDesc(existing.getId(), c.getId()));
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
                .phone(PHONE).name("Share Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone("9800000002").name("Share Client")
                .status(Client.Status.ACTIVE).build());

        mealPlan = mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId()).clientId(client.getId())
                .name("7-Day Plan").status(MealPlan.Status.ACTIVE)
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void shareWhatsapp_validPlan_returns200AndLogsNotification() throws Exception {
        mockMvc.perform(post("/api/v1/meal-plans/{planId}/share/whatsapp", mealPlan.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("shared")));

        // Notification log should be persisted
        var logs = notificationLogRepository.findByCoachIdAndClientIdOrderByCreatedAtDesc(
                coach.getId(), client.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatus().name()).isEqualTo("SENT");
    }

    @Test
    void shareWhatsapp_planNotOwned_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/meal-plans/{planId}/share/whatsapp", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shareWhatsapp_wrongCoach_returns404() throws Exception {
        Coach other = coachRepository.save(Coach.builder()
                .phone("9800000099").name("Other")
                .trialEndsAt(Instant.now().plusSeconds(86400L)).build());
        String otherJwt = jwtService.generateToken(other.getPhone(), other.getId(), "ROLE_COACH");

        mockMvc.perform(post("/api/v1/meal-plans/{planId}/share/whatsapp", mealPlan.getId())
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());

        coachRepository.delete(other);
    }

    @Test
    void shareWhatsapp_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/meal-plans/{planId}/share/whatsapp", mealPlan.getId()))
                .andExpect(status().isUnauthorized());
    }
}
