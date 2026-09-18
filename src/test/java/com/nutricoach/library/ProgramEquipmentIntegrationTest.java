package com.nutricoach.library;

import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.*;
import com.nutricoach.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The derived {@code equipment} column on the Programs list.
 *
 * <p>Equipment is not stored on a program — it is walked from the program's days
 * through their workouts to the exercises inside, so these tests build that whole
 * chain rather than asserting on a stored field.
 */
class ProgramEquipmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CoachRepository coachRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramDayRepository programDayRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired WorkoutSectionRepository sectionRepository;
    @Autowired WorkoutSectionAssignmentRepository sectionAssignmentRepository;
    @Autowired WorkoutSectionExerciseRepository sectionExerciseRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        cleanup("9800070001");
        coach = coachRepository.save(Coach.builder()
                .phone("9800070001").name("Equipment Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            UUID id = existing.getId();
            sectionExerciseRepository.deleteAll(sectionExerciseRepository.findAll().stream()
                    .filter(se -> sectionRepository.findById(se.getSectionId())
                            .map(s -> s.getCoachId().equals(id)).orElse(false)).toList());
            sectionAssignmentRepository.deleteAll(sectionAssignmentRepository.findAll().stream()
                    .filter(sa -> workoutRepository.findById(sa.getWorkoutId())
                            .map(w -> w.getCoachId().equals(id)).orElse(false)).toList());
            programRepository.findAll().stream()
                    .filter(p -> p.getCoachId().equals(id))
                    .forEach(p -> {
                        programDayRepository.deleteAll(
                                programDayRepository.findByProgramIdOrderByDayNumberAsc(p.getId()));
                        programRepository.delete(p);
                    });
            sectionRepository.deleteAll(sectionRepository.findAll().stream()
                    .filter(s -> s.getCoachId().equals(id)).toList());
            workoutRepository.deleteAll(workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(id)).toList());
            exerciseRepository.deleteAll(exerciseRepository.findAll().stream()
                    .filter(e -> e.getCoachId().equals(id)).toList());
            coachRepository.delete(existing);
        });
    }

    /** Build workout → section → exercise, and return the workout. */
    private Workout workoutUsing(String workoutName, String exerciseName, String equipment) {
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name(exerciseName).equipment(equipment).build());
        WorkoutSection section = sectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name(exerciseName + " block")
                .sectionType(WorkoutSection.Type.MAIN).build());
        sectionExerciseRepository.save(WorkoutSectionExercise.builder()
                .sectionId(section.getId()).exerciseId(exercise.getId()).position(0).build());
        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name(workoutName).build());
        sectionAssignmentRepository.save(WorkoutSectionAssignment.builder()
                .workoutId(workout.getId()).sectionId(section.getId()).position(0).build());
        return workout;
    }

    private Program programWithDays(String name, Workout... workouts) {
        Program program = programRepository.save(Program.builder()
                .coachId(coach.getId()).name(name).weeks(1).durationDays(7).build());
        int day = 1;
        for (Workout w : workouts) {
            programDayRepository.save(ProgramDay.builder()
                    .programId(program.getId()).dayNumber(day++).workoutId(w.getId()).build());
        }
        return program;
    }

    @Test
    void list_derivesEquipmentFromTheExercisesInTheProgramsWorkouts() throws Exception {
        programWithDays("Strength Base",
                workoutUsing("Push Day", "Bench Press", "Barbell"),
                workoutUsing("Pull Day", "Row", "Dumbbell"));

        mockMvc.perform(get("/api/v1/library/programs").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipment.length()").value(2))
                .andExpect(jsonPath("$.data[0].equipment[0]").value("Barbell"))
                .andExpect(jsonPath("$.data[0].equipment[1]").value("Dumbbell"));
    }

    /** Two days on the same equipment list it once, not twice. */
    @Test
    void list_deduplicatesEquipmentAcrossDays() throws Exception {
        programWithDays("Barbell Only",
                workoutUsing("Day A", "Squat", "Barbell"),
                workoutUsing("Day B", "Deadlift", "Barbell"));

        mockMvc.perform(get("/api/v1/library/programs").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipment.length()").value(1))
                .andExpect(jsonPath("$.data[0].equipment[0]").value("Barbell"));
    }

    @Test
    void list_withNoDays_returnsEmptyEquipment() throws Exception {
        programRepository.save(Program.builder()
                .coachId(coach.getId()).name("Empty Program").weeks(1).durationDays(7).build());

        mockMvc.perform(get("/api/v1/library/programs").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipment.length()").value(0));
    }

    /** Bodyweight exercises record no equipment and must not produce a blank chip. */
    @Test
    void list_ignoresExercisesWithNoEquipment() throws Exception {
        programWithDays("Bodyweight",
                workoutUsing("Day A", "Push Up", null),
                workoutUsing("Day B", "Plank", "   "));

        mockMvc.perform(get("/api/v1/library/programs").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipment.length()").value(0));
    }

    @Test
    void list_ignoresEquipmentFromDeletedExercises() throws Exception {
        Workout workout = workoutUsing("Day A", "Bench Press", "Barbell");
        programWithDays("Has Deleted Exercise", workout);
        Exercise exercise = exerciseRepository.findAll().stream()
                .filter(e -> e.getCoachId().equals(coach.getId())).findFirst().orElseThrow();
        exercise.setDeletedAt(Instant.now());
        exerciseRepository.save(exercise);

        mockMvc.perform(get("/api/v1/library/programs").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipment.length()").value(0));
    }

    @Test
    void list_ignoresEquipmentFromDeletedWorkouts() throws Exception {
        Workout workout = workoutUsing("Day A", "Bench Press", "Barbell");
        programWithDays("Has Deleted Workout", workout);
        workout.setDeletedAt(Instant.now());
        workoutRepository.save(workout);

        mockMvc.perform(get("/api/v1/library/programs").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].equipment.length()").value(0));
    }
}
