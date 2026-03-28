package com.nutricoach.plans;

import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.repository.FoodItemRepository;
import com.nutricoach.progress.repository.ProgressLogRepository;
import com.nutricoach.progress.repository.ProgressPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FoodItemIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired FoodItemRepository foodItemRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired ProgressPhotoRepository progressPhotoRepository;
    @Autowired JwtService jwtService;

    private String jwt;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9700000001").ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId())
                        .forEach(l -> progressPhotoRepository.deleteAll(
                                progressPhotoRepository.findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(existing.getId(), l.getId())));
                progressLogRepository.deleteAll(
                        progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId()));
            });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        Coach coach = coachRepository.save(Coach.builder()
                .phone("9700000001")
                .name("Food Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void search_noParams_returnsSeedItems() throws Exception {
        mockMvc.perform(get("/api/v1/food-items")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void search_byQuery_filtersResults() throws Exception {
        mockMvc.perform(get("/api/v1/food-items?q=rice")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void search_byCuisine_filtersResults() throws Exception {
        mockMvc.perform(get("/api/v1/food-items?cuisine=SOUTH_INDIAN")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void search_byCategory_filtersResults() throws Exception {
        mockMvc.perform(get("/api/v1/food-items?category=GRAIN")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void search_byCuisineAndCategory_filtersResults() throws Exception {
        mockMvc.perform(get("/api/v1/food-items?cuisine=PAN_INDIAN&category=PROTEIN")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getById_validId_returns200() throws Exception {
        UUID id = foodItemRepository.search("rice", null, null)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing"))
                .getId();

        mockMvc.perform(get("/api/v1/food-items/{id}", id)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.caloriesPer100g").exists());
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/food-items/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/food-items"))
                .andExpect(status().isUnauthorized());
    }
}
