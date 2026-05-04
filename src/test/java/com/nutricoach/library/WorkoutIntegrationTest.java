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
import java.util.List;
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

    @Test
    void duplicateWorkout_copiesMetadataAndAssignments() throws Exception {
        String workoutId = createWorkout("Original Workout");
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Shared Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        mockMvc.perform(post("/api/v1/library/workouts/{id}/sections", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", section.getId().toString()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/library/workouts/{id}/duplicate", workoutId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Original Workout (copy)"))
                .andExpect(jsonPath("$.data.sections.length()").value(1))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void duplicateWorkout_unknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts/{id}/duplicate", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWorkout_withTags_roundTrips() throws Exception {
        mockMvc.perform(post("/api/v1/library/workouts")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Tagged Workout",
                                "tags", List.of("hypertrophy", "upper")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tags[0]").value("hypertrophy"));
    }

    @Test
    void createSection_finisherType_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/library/workout-sections")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Finisher",
                                "sectionType", "FINISHER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sectionType").value("FINISHER"));
    }

    @Test
    void addSectionExercise_withWeight_roundTrips() throws Exception {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name("Bench Press").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Push Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", ex.getId().toString(),
                                "sets", 5, "reps", 5,
                                "weight", "70 kg"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.exercises[0].weight").value("70 kg"));
    }

    @Test
    void patchSectionExercise_updatesParams() throws Exception {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name("Squat").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Legs")
                .sectionType(WorkoutSection.Type.MAIN).build());

        String addResponse = mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", ex.getId().toString(), "sets", 3, "reps", 8))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String entryId = objectMapper.readTree(addResponse).path("data").path("exercises").get(0).path("id").asText();

        mockMvc.perform(patch("/api/v1/library/workout-sections/{id}/exercises/{entryId}",
                        section.getId(), entryId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sets", 5, "reps", 5, "weight", "80 kg", "restSeconds", 90))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exercises[0].sets").value(5))
                .andExpect(jsonPath("$.data.exercises[0].reps").value(5))
                .andExpect(jsonPath("$.data.exercises[0].weight").value("80 kg"))
                .andExpect(jsonPath("$.data.exercises[0].restSeconds").value(90));
    }

    @Test
    void reorderSectionExercises_updatesPositions() throws Exception {
        Exercise a = exerciseRepository.save(Exercise.builder().coachId(coach.getId()).name("A").build());
        Exercise b = exerciseRepository.save(Exercise.builder().coachId(coach.getId()).name("B").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Order Section")
                .sectionType(WorkoutSection.Type.MAIN).build());

        String r1 = mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", a.getId().toString()))))
                .andReturn().getResponse().getContentAsString();
        String r2 = mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", b.getId().toString()))))
                .andReturn().getResponse().getContentAsString();
        String idA = objectMapper.readTree(r1).path("data").path("exercises").get(0).path("id").asText();
        String idB = objectMapper.readTree(r2).path("data").path("exercises").get(1).path("id").asText();

        mockMvc.perform(put("/api/v1/library/workout-sections/{id}/exercises/order", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderedIds", List.of(idB, idA)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exercises[0].id").value(idB))
                .andExpect(jsonPath("$.data.exercises[1].id").value(idA));
    }

    @Test
    void reorderSectionExercises_partialList_returns400() throws Exception {
        Exercise a = exerciseRepository.save(Exercise.builder().coachId(coach.getId()).name("A").build());
        Exercise b = exerciseRepository.save(Exercise.builder().coachId(coach.getId()).name("B").build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Bad Order")
                .sectionType(WorkoutSection.Type.MAIN).build());

        String r1 = mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", a.getId().toString()))))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/v1/library/workout-sections/{id}/exercises", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("exerciseId", b.getId().toString()))))
                .andExpect(status().isCreated());
        String idA = objectMapper.readTree(r1).path("data").path("exercises").get(0).path("id").asText();

        mockMvc.perform(put("/api/v1/library/workout-sections/{id}/exercises/order", section.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderedIds", List.of(idA)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reorderWorkoutSections_updatesPositions() throws Exception {
        String workoutId = createWorkout("Reorder Workout");
        WorkoutSection s1 = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("First").sectionType(WorkoutSection.Type.WARM_UP).build());
        WorkoutSection s2 = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Second").sectionType(WorkoutSection.Type.MAIN).build());

        String a1 = mockMvc.perform(post("/api/v1/library/workouts/{id}/sections", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", s1.getId().toString()))))
                .andReturn().getResponse().getContentAsString();
        String a2 = mockMvc.perform(post("/api/v1/library/workouts/{id}/sections", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sectionId", s2.getId().toString()))))
                .andReturn().getResponse().getContentAsString();
        String assignmentId1 = objectMapper.readTree(a1).path("data").path("sections").get(0).path("assignmentId").asText();
        String assignmentId2 = objectMapper.readTree(a2).path("data").path("sections").get(1).path("assignmentId").asText();

        mockMvc.perform(put("/api/v1/library/workouts/{id}/sections/order", workoutId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderedIds", List.of(assignmentId2, assignmentId1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections[0].assignmentId").value(assignmentId2))
                .andExpect(jsonPath("$.data.sections[1].assignmentId").value(assignmentId1));
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
