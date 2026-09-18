package com.nutricoach.library;

import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.ClientProgramAssignment;
import com.nutricoach.library.entity.ClientWorkoutAssignment;
import com.nutricoach.library.entity.Program;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GET /api/v1/library/clients/{clientId}/assignments — the read behind the
 * coach-side Training tab.
 */
class ClientTrainingIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired ClientProgramAssignmentRepository programAssignmentRepository;
    @Autowired ClientWorkoutAssignmentRepository workoutAssignmentRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private Client rivalClient;
    private Program program;
    private Workout workout;

    @BeforeEach
    void setup() {
        cleanup("9800060001");
        cleanup("9800060002");

        coach = coachRepository.save(Coach.builder()
                .phone("9800060001").name("Training Tab Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        Coach rivalCoach = coachRepository.save(Coach.builder()
                .phone("9800060002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Training Client").phone("9122221111")
                .status(Client.Status.ACTIVE).build());
        rivalClient = clientRepository.save(Client.builder()
                .coachId(rivalCoach.getId()).name("Rival Client").phone("9122221112")
                .status(Client.Status.ACTIVE).build());

        program = programRepository.save(Program.builder()
                .coachId(coach.getId()).name("8-Week Hypertrophy")
                .weeks(8).durationDays(56).build());
        workout = workoutRepository.save(Workout.builder()
                .coachId(coach.getId()).name("Push Day A").build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            programAssignmentRepository.findAll().stream()
                    .filter(a -> a.getCoachId().equals(existing.getId()))
                    .forEach(programAssignmentRepository::delete);
            workoutAssignmentRepository.findAll().stream()
                    .filter(a -> a.getCoachId().equals(existing.getId()))
                    .forEach(workoutAssignmentRepository::delete);
            programRepository.findAll().stream()
                    .filter(p -> p.getCoachId().equals(existing.getId()))
                    .forEach(programRepository::delete);
            workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(existing.getId()))
                    .forEach(workoutRepository::delete);
            clientRepository.findAll().stream()
                    .filter(c -> c.getCoachId().equals(existing.getId()))
                    .forEach(clientRepository::delete);
            coachRepository.delete(existing);
        });
    }

    private ClientProgramAssignment assignProgram(LocalDate startDate) {
        return programAssignmentRepository.save(ClientProgramAssignment.builder()
                .coachId(coach.getId()).clientId(client.getId()).programId(program.getId())
                .assignedAt(Instant.now()).startDate(startDate).build());
    }

    private ClientWorkoutAssignment assignWorkout() {
        return workoutAssignmentRepository.save(ClientWorkoutAssignment.builder()
                .coachId(coach.getId()).clientId(client.getId()).workoutId(workout.getId())
                .assignedAt(Instant.now()).build());
    }

    @Test
    void listAssignments_returnsProgramsAndWorkoutsWithNames() throws Exception {
        assignProgram(LocalDate.of(2026, 9, 12));
        assignWorkout();

        mockMvc.perform(get("/api/v1/library/clients/{id}/assignments", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.programs.length()").value(1))
                .andExpect(jsonPath("$.data.programs[0].name").value("8-Week Hypertrophy"))
                .andExpect(jsonPath("$.data.programs[0].weeks").value(8))
                .andExpect(jsonPath("$.data.programs[0].startDate").value("2026-09-12"))
                .andExpect(jsonPath("$.data.workouts.length()").value(1))
                .andExpect(jsonPath("$.data.workouts[0].name").value("Push Day A"));
    }

    @Test
    void listAssignments_withNothingAssigned_returnsEmptyLists() throws Exception {
        mockMvc.perform(get("/api/v1/library/clients/{id}/assignments", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.programs.length()").value(0))
                .andExpect(jsonPath("$.data.workouts.length()").value(0));
    }

    @Test
    void listAssignments_excludesUnassignedRows() throws Exception {
        ClientProgramAssignment a = assignProgram(LocalDate.of(2026, 9, 12));
        a.setDeletedAt(Instant.now());
        programAssignmentRepository.save(a);

        mockMvc.perform(get("/api/v1/library/clients/{id}/assignments", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.programs.length()").value(0));
    }

    /** A program deleted from the library leaves no nameless row behind. */
    @Test
    void listAssignments_skipsAssignmentsWhoseProgramWasDeleted() throws Exception {
        assignProgram(LocalDate.of(2026, 9, 12));
        program.setDeletedAt(Instant.now());
        programRepository.save(program);

        mockMvc.perform(get("/api/v1/library/clients/{id}/assignments", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.programs.length()").value(0));
    }

    @Test
    void listAssignments_forAnotherCoachsClient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/library/clients/{id}/assignments", rivalClient.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void listAssignments_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/library/clients/{id}/assignments", client.getId()))
                .andExpect(status().isUnauthorized());
    }
}
