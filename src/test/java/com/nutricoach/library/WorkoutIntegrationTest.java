package com.nutricoach.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.Exercise;
import com.nutricoach.library.entity.WorkoutSection;
import com.nutricoach.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkoutIntegrationTest extends AbstractIntegrationTest {

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
        coachRepository.findByPhone("9800020001").ifPresent(existing -> {
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
                .phone("9800020001").name("Workout Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());
        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void createWorkout_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Full Body Blast",
                                "estimatedDurationMinutes", 60))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Full Body Blast"))
                .andExpect(jsonPath("$.data.estimatedDurationMinutes").value(60));
    }

    @Test
    void createSection_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/workout-sections")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Dynamic Warm-up",
                                "sectionType", "WARM_UP"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sectionType").value("WARM_UP"));
    }

    @Test
    void createSection_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/library/workout-sections")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Missing Type Section"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addExerciseToSection_returnsUpdatedSection() throws Exception {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name("Lunge").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Legs Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", ex.getId().toString(),
                                "sets", 3, "reps", 12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.exercises.length()").value(1))
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Lunge"))
                .andExpect(jsonPath("$.data.exercises[0].sets").value(3));
    }

    @Test
    void attachSectionToWorkout_returnsWorkoutWithSection() throws Exception {
        String workoutId = createWorkout("Test Workout");
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Cool Down Stretch")
                .sectionType(WorkoutSection.Type.COOL_DOWN).build());

        mockMvc.perform(post("/api/v1/library/workouts/{id}/sections", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sectionId", section.getId().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sections.length()").value(1))
                .andExpect(jsonPath("$.data.sections[0].name").value("Cool Down Stretch"));
    }

    @Test
    void deleteSection_usedByWorkout_returns409() throws Exception {
        String workoutId = createWorkout("Using Workout");
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("In-use Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        mockMvc.perform(post("/api/v1/library/workouts/{id}/sections", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sectionId", section.getId().toString()))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/library/workout-sections/{id}", section.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isConflict());
    }

    @Test
    void listWorkouts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/library/workouts"))
                .andExpect(status().isUnauthorized());
    }

    private String createWorkout(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/library/workouts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asText();
    }
}
