package com.nutricoach.portal;

import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PortalWorkoutIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired WorkoutSectionRepository workoutSectionRepository;
    @Autowired WorkoutSectionAssignmentRepository sectionAssignmentRepository;
    @Autowired WorkoutSectionExerciseRepository sectionExerciseRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramDayRepository programDayRepository;
    @Autowired ClientProgramAssignmentRepository assignmentRepository;
    @Autowired JwtService jwtService;

    private Coach coach;
    private Client client;
    private Program program;
    private String clientJwt;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9700050001").ifPresent(existing -> {
            assignmentRepository.deleteAll(assignmentRepository.findAll().stream()
                    .filter(a -> a.getCoachId().equals(existing.getId())).toList());
            programRepository.findAll().stream().filter(p -> p.getCoachId().equals(existing.getId()))
                    .forEach(p -> {
                        programDayRepository.deleteAll(programDayRepository.findByProgramIdOrderByDayNumberAsc(p.getId()));
                        programRepository.delete(p);
                    });
            workoutRepository.findAll().stream().filter(w -> w.getCoachId().equals(existing.getId()))
                    .forEach(w -> {
                        sectionAssignmentRepository.deleteAll(sectionAssignmentRepository.findByWorkoutIdOrderByPositionAsc(w.getId()));
                        workoutRepository.delete(w);
                    });
            workoutSectionRepository.findAll().stream().filter(s -> s.getCoachId().equals(existing.getId()))
                    .forEach(s -> {
                        sectionExerciseRepository.deleteAll(sectionExerciseRepository.findBySectionIdOrderByPositionAsc(s.getId()));
                        workoutSectionRepository.delete(s);
                    });
            exerciseRepository.findAll().stream().filter(e -> e.getCoachId().equals(existing.getId()))
                    .forEach(exerciseRepository::delete);
            clientRepository.findAllByCoachId(existing.getId()).forEach(clientRepository::delete);
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9700050001").name("Portal Workout Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone("9700050002").name("Portal Workout Client")
                .status(Client.Status.ACTIVE).build());
        clientJwt = jwtService.generateClientToken(client.getPhone(), client.getId(), coach.getId());

        // Workout: one section with one exercise (3x10).
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .coachId(coach.getId()).name("Barbell Squat").category(Exercise.Category.strength).build());
        Workout workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name("Leg Day").build());
        WorkoutSection section = workoutSectionRepository.save(WorkoutSection.builder()
                .coachId(coach.getId()).name("Main").sectionType(WorkoutSection.Type.MAIN).build());
        sectionAssignmentRepository.save(WorkoutSectionAssignment.builder()
                .workoutId(workout.getId()).sectionId(section.getId()).position(0).build());
        sectionExerciseRepository.save(WorkoutSectionExercise.builder()
                .sectionId(section.getId()).exerciseId(exercise.getId()).position(0)
                .sets(3).reps(10).weight("100kg").build());

        // 4-week program; day 1 -> the workout.
        program = programRepository.save(Program.builder()
                .coachId(coach.getId()).name("Strength Base").weeks(4).durationDays(28).build());
        programDayRepository.save(ProgramDay.builder()
                .programId(program.getId()).dayNumber(1).workoutId(workout.getId()).build());
    }

    @Test
    void upcomingWorkouts_startToday_returnsDatedWorkoutWithExercises() throws Exception {
        assignmentRepository.save(ClientProgramAssignment.builder()
                .coachId(coach.getId()).clientId(client.getId()).programId(program.getId())
                .assignedAt(Instant.now()).startDate(LocalDate.now()).build());

        mockMvc.perform(get("/api/v1/portal/workouts").header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].workoutName").value("Leg Day"))
                .andExpect(jsonPath("$.data[0].programName").value("Strength Base"))
                .andExpect(jsonPath("$.data[0].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.data[0].exerciseCount").value(1))
                .andExpect(jsonPath("$.data[0].exercises[0].name").value("Barbell Squat"))
                .andExpect(jsonPath("$.data[0].exercises[0].sets").value(3))
                .andExpect(jsonPath("$.data[0].exercises[0].target").value("100kg x 10"));
    }

    @Test
    void upcomingWorkouts_startInPast_excludesElapsedDays() throws Exception {
        // Day 1 started 3 days ago -> in the past, should be excluded.
        assignmentRepository.save(ClientProgramAssignment.builder()
                .coachId(coach.getId()).clientId(client.getId()).programId(program.getId())
                .assignedAt(Instant.now()).startDate(LocalDate.now().minusDays(3)).build());

        mockMvc.perform(get("/api/v1/portal/workouts").header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void upcomingWorkouts_noAssignment_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/portal/workouts").header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void upcomingWorkouts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/portal/workouts"))
                .andExpect(status().isUnauthorized());
    }
}
