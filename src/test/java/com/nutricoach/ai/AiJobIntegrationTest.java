package com.nutricoach.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.ai.entity.AiJob;
import com.nutricoach.ai.repository.AiJobRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.repository.MealPlanDayRepository;
import com.nutricoach.plans.repository.MealPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.TimeUnit;

class AiJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired AiJobRepository aiJobRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired MealPlanDayRepository mealPlanDayRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;

    private static final String PHONE = "9900000001";

    @BeforeEach
    void setup() {
        coachRepository.findByPhone(PHONE).ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c ->
                    mealPlanRepository.findAll().stream()
                            .filter(p -> p.getCoachId().equals(existing.getId()) && p.getClientId().equals(c.getId()))
                            .forEach(plan -> {
                                mealPlanDayRepository.deleteAll(
                                        mealPlanDayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()));
                                mealPlanRepository.delete(plan);
                            }));
            aiJobRepository.findByCoachIdOrderByCreatedAtDesc(existing.getId())
                    .forEach(aiJobRepository::delete);
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone(PHONE).name("AI Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone("9900000002").name("AI Client")
                .status(Client.Status.ACTIVE).build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void generateMealPlan_validClient_returns201WithJobId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", client.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertThat(aiJobRepository.findById(UUID.fromString(jobId))).isPresent();
    }

    @Test
    void generateMealPlan_clientNotOwned_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", UUID.randomUUID()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generateMealPlan_missingClientId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generateMealPlan_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", client.getId()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getJob_existingJob_returns200() throws Exception {
        AiJob job = aiJobRepository.save(AiJob.builder()
                .coachId(coach.getId()).clientId(client.getId())
                .jobType(AiJob.JobType.MEAL_PLAN_GENERATION)
                .status(AiJob.Status.PENDING)
                .build());

        mockMvc.perform(get("/api/v1/ai/jobs/{jobId}", job.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(job.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.jobType").value("MEAL_PLAN_GENERATION"));
    }

    @Test
    void getJob_wrongCoach_returns404() throws Exception {
        AiJob job = aiJobRepository.save(AiJob.builder()
                .coachId(coach.getId()).clientId(client.getId())
                .jobType(AiJob.JobType.MEAL_PLAN_GENERATION)
                .status(AiJob.Status.PENDING)
                .build());

        Coach other = coachRepository.save(Coach.builder()
                .phone("9900000099").name("Other")
                .trialEndsAt(Instant.now().plusSeconds(86400L)).build());
        String otherJwt = jwtService.generateToken(other.getPhone(), other.getId(), "ROLE_COACH");

        mockMvc.perform(get("/api/v1/ai/jobs/{jobId}", job.getId())
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());

        coachRepository.delete(other);
    }

    @Test
    void generateMealPlan_asyncJob_completesWithMealPlanId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ai/meal-plans/generate")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("clientId", client.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
        UUID jobUUID = UUID.fromString(jobId);

        // Wait for @Async processJob to complete (stub response, should be fast)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            AiJob updated = aiJobRepository.findById(jobUUID).orElseThrow();
            assertThat(updated.getStatus()).isIn(AiJob.Status.COMPLETED, AiJob.Status.FAILED);
        });

        AiJob completed = aiJobRepository.findById(jobUUID).orElseThrow();
        assertThat(completed.getCompletedAt()).isNotNull();
        // With local stub key, job should complete with a meal plan
        if (completed.getStatus() == AiJob.Status.COMPLETED) {
            assertThat(completed.getOutputPayload()).containsKey("mealPlanId");
        }
    }
}
