package com.nutricoach.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkoutTemplateIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired WorkoutSectionRepository sectionRepository;
    @Autowired WorkoutSectionExerciseRepository sectionExerciseRepository;
    @Autowired WorkoutSectionAssignmentRepository assignmentRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9800050001").ifPresent(existing -> {
            assignmentRepository.findAll().stream()
                    .filter(a -> workoutRepository.findById(a.getWorkoutId())
                            .map(w -> w.getCoachId().equals(existing.getId())).orElse(false))
                    .forEach(assignmentRepository::delete);
            workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(existing.getId()))
                    .forEach(workoutRepository::delete);
            sectionExerciseRepository.findAll().stream()
                    .filter(se -> sectionRepository.findById(se.getSectionId())
                            .map(s -> s.getCoachId().equals(existing.getId())).orElse(false))
                    .forEach(sectionExerciseRepository::delete);
            sectionRepository.findAll().stream()
                    .filter(s -> s.getCoachId().equals(existing.getId()))
                    .forEach(sectionRepository::delete);
            exerciseRepository.findAll().stream()
                    .filter(e -> e.getCoachId().equals(existing.getId()))
                    .forEach(exerciseRepository::delete);
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9800050001").name("Template Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());
        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void list_returnsSeededTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/library/workout-templates")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(6)));
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/library/workout-templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void instantiate_createsWorkoutWithSectionsAndExercises() throws Exception {
        String listResponse = mockMvc.perform(get("/api/v1/library/workout-templates")
                        .header("Authorization", "Bearer " + jwt))
                .andReturn().getResponse().getContentAsString();
        JsonNode templates = objectMapper.readTree(listResponse).path("data");
        String templateId = templates.get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/library/workout-templates/{id}/instantiate", templateId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.sections.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.sections[0].exercises.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void instantiate_unknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/library/workout-templates/{id}/instantiate", java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
