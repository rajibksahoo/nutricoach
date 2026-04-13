package com.nutricoach.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.repository.ProgramDayRepository;
import com.nutricoach.library.repository.ProgramRepository;
import com.nutricoach.library.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProgramIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramDayRepository programDayRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9800030001").ifPresent(existing -> {
            programRepository.findAll().stream()
                    .filter(p -> p.getCoachId().equals(existing.getId()))
                    .forEach(p -> {
                        programDayRepository.deleteAll(
                                programDayRepository.findByProgramIdOrderByDayNumberAsc(p.getId()));
                        programRepository.delete(p);
                    });
            workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(existing.getId()))
                    .forEach(workoutRepository::delete);
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9800030001").name("Program Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());
        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/programs")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "30-day Kickstart",
                                "durationDays", 30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.durationDays").value(30));
    }

    @Test
    void create_durationZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/library/programs")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Bad Program",
                                "durationDays", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setDay_assignsWorkout() throws Exception {
        String programId = createProgram("Assign Test", 30);
        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name("Day 1 Workout").build());

        Map<String, Object> body = new HashMap<>();
        body.put("workoutId", workout.getId().toString());
        body.put("notes", "Warm up slow");

        mockMvc.perform(put("/api/v1/library/programs/{id}/days/{day}", programId, 1)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(1))
                .andExpect(jsonPath("$.data.days[0].dayNumber").value(1))
                .andExpect(jsonPath("$.data.days[0].workoutName").value("Day 1 Workout"));
    }

    @Test
    void setDay_beyondDuration_returns400() throws Exception {
        String programId = createProgram("Short Program", 7);

        Map<String, Object> body = new HashMap<>();
        body.put("workoutId", null);

        mockMvc.perform(put("/api/v1/library/programs/{id}/days/{day}", programId, 99)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clearDay_removesAssignment() throws Exception {
        String programId = createProgram("Clear Test", 30);
        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name("Temp Workout").build());

        Map<String, Object> body = new HashMap<>();
        body.put("workoutId", workout.getId().toString());

        mockMvc.perform(put("/api/v1/library/programs/{id}/days/{day}", programId, 5)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/library/programs/{id}/days/{day}", programId, 5)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days.length()").value(0));
    }

    @Test
    void update_shrinkBelowAssignedDay_returns409() throws Exception {
        String programId = createProgram("Shrink Test", 30);
        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name("Any Workout").build());

        Map<String, Object> assign = new HashMap<>();
        assign.put("workoutId", workout.getId().toString());

        mockMvc.perform(put("/api/v1/library/programs/{id}/days/{day}", programId, 20)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/library/programs/{id}", programId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durationDays", 10))))
                .andExpect(status().isConflict());
    }

    @Test
    void get_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/library/programs/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/library/programs"))
                .andExpect(status().isUnauthorized());
    }

    private String createProgram(String name, int durationDays) throws Exception {
        String response = mockMvc.perform(post("/api/v1/library/programs")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "durationDays", durationDays))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
