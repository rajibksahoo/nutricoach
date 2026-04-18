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
import java.util.UUID;

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

    @Test
    void listWorkouts_authenticated_returnsOnlyCoachWorkouts() throws Exception {
        createWorkout("Push Day");
        createWorkout("Pull Day");

        mockMvc.perform(get("/api/v1/library/workouts")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getWorkout_byId_returnsFullWorkout() throws Exception {
        String id = createWorkout("Leg Day");

        mockMvc.perform(get("/api/v1/library/workouts/{id}", id)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Leg Day"))
                .andExpect(jsonPath("$.data.sections").isArray());
    }

    @Test
    void getWorkout_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/library/workouts/{id}", java.util.UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWorkout_patchesNameAndDuration() throws Exception {
        String id = createWorkout("Old Name");

        mockMvc.perform(put("/api/v1/library/workouts/{id}", id)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "New Name", "estimatedDurationMinutes", 45))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.estimatedDurationMinutes").value(45));
    }

    @Test
    void deleteWorkout_softDelete_notInListAfterwards() throws Exception {
        String id = createWorkout("To Delete");

        mockMvc.perform(delete("/api/v1/library/workouts/{id}", id)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/workouts")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void detachSection_removesFromWorkout() throws Exception {
        String workoutId = createWorkout("Detach Test");
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Detachable Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        String attachResponse = mockMvc.perform(post("/api/v1/library/workouts/{id}/sections", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", section.getId().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String assignmentId = objectMapper.readTree(attachResponse)
                .path("data").path("sections").get(0).path("assignmentId").asText();

        mockMvc.perform(delete("/api/v1/library/workouts/{id}/sections/{assignmentId}", workoutId, assignmentId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/workouts/{id}", workoutId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections.length()").value(0));
    }

    @Test
    void listSections_authenticated_returnsSections() throws Exception {
        sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Morning Warm-up")
                .sectionType(WorkoutSection.Type.WARM_UP).build());

        mockMvc.perform(get("/api/v1/library/workout-sections")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Morning Warm-up"));
    }

    @Test
    void getSection_byId_returnsSectionWithExercises() throws Exception {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name("Plank").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Core Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", ex.getId().toString(), "sets", 3, "reps", 30))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/library/workout-sections/{id}", section.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Core Section"))
                .andExpect(jsonPath("$.data.exercises.length()").value(1))
                .andExpect(jsonPath("$.data.exercises[0].exerciseName").value("Plank"));
    }

    @Test
    void getSection_otherCoachSection_returns404() throws Exception {
        Coach other = coachRepository.save(Coach.builder()
                .phone("9800020099").name("Other Coach")
                .trialEndsAt(java.time.Instant.now().plusSeconds(86400L))
                .build());
        WorkoutSection otherSection = sectionRepository.save(WorkoutSection.builder()
                .coachId(other.getId()).name("Their Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        mockMvc.perform(get("/api/v1/library/workout-sections/{id}", otherSection.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());

        sectionRepository.delete(otherSection);
        coachRepository.delete(other);
    }

    @Test
    void updateSection_patchesNameAndType() throws Exception {
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Orig Name")
                .sectionType(WorkoutSection.Type.WARM_UP).build());

        mockMvc.perform(put("/api/v1/library/workout-sections/{id}", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Updated Name", "sectionType", "COOL_DOWN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.sectionType").value("COOL_DOWN"));
    }

    @Test
    void deleteSection_notInUse_softDeletes() throws Exception {
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Unused Section")
                .sectionType(WorkoutSection.Type.COOL_DOWN).build());

        mockMvc.perform(delete("/api/v1/library/workout-sections/{id}", section.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/workout-sections")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void removeSectionExercise_removesFromSection() throws Exception {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name("Burpee").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("HIIT Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        String addResponse = mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", ex.getId().toString(), "sets", 4, "reps", 10))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String entryId = objectMapper.readTree(addResponse)
                .path("data").path("exercises").get(0).path("id").asText();

        mockMvc.perform(delete("/api/v1/library/workout-sections/{id}/exercises/{entryId}",
                        section.getId(), entryId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/library/workout-sections/{id}", section.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exercises.length()").value(0));
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
