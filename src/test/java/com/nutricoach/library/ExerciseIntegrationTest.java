package com.nutricoach.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.Exercise;
import com.nutricoach.library.repository.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExerciseIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Coach otherCoach;

    @BeforeEach
    void setup() {
        cleanup("9800010001");
        cleanup("9800010002");

        coach = coachRepository.save(Coach.builder()
                .phone("9800010001").name("Exercise Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        otherCoach = coachRepository.save(Coach.builder()
                .phone("9800010002").name("Other Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            exerciseRepository.findAll().stream()
                    .filter(e -> e.getCoachId().equals(existing.getId()))
                    .forEach(exerciseRepository::delete);
            coachRepository.delete(existing);
        });
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/exercises")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Push-up",
                                "muscleGroup", "Chest",
                                "equipment", "Bodyweight"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Push-up"))
                .andExpect(jsonPath("$.data.muscleGroup").value("Chest"));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/library/exercises")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("muscleGroup", "Legs"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/library/exercises"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsOnlyCurrentCoachItems() throws Exception {
        exerciseRepository.save(Exercise.builder().coachId(coach.getId()).name("Squat").build());
        exerciseRepository.save(Exercise.builder().coachId(otherCoach.getId()).name("Deadlift").build());

        mockMvc.perform(get("/api/v1/library/exercises")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[?(@.name == 'Deadlift')]").doesNotExist());
    }

    @Test
    void get_otherCoachExercise_returns404() throws Exception {
        Exercise other = exerciseRepository.save(
                Exercise.builder().coachId(otherCoach.getId()).name("Private").build());

        mockMvc.perform(get("/api/v1/library/exercises/{id}", other.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_patchesName() throws Exception {
        String id = createAndGetId("Original");

        mockMvc.perform(put("/api/v1/library/exercises/{id}", id)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Renamed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed"));
    }

    @Test
    void delete_softDeletes_notInList() throws Exception {
        String id = createAndGetId("To Delete");

        mockMvc.perform(delete("/api/v1/library/exercises/{id}", id)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/exercises")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test
    void get_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/library/exercises/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    private String createAndGetId(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/library/exercises")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
