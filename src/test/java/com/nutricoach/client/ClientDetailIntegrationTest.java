package com.nutricoach.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientNoteRepository;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.library.entity.*;
import com.nutricoach.library.repository.*;
import com.nutricoach.progress.entity.ProgressLog;
import com.nutricoach.progress.repository.ProgressLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The client-detail read models: Training stats, the Updates feed, and notes.
 */
class ClientDetailIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ClientNoteRepository noteRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramDayRepository programDayRepository;
    @Autowired ClientProgramAssignmentRepository programAssignmentRepository;
    @Autowired ClientWorkoutCompletionRepository completionRepository;
    @Autowired ClientWorkoutScheduleRepository scheduleRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private Client rivalClient;

    @BeforeEach
    void setup() {
        cleanup("9800090001");
        cleanup("9800090002");

        coach = coachRepository.save(Coach.builder()
                .phone("9800090001").name("Detail Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        Coach rivalCoach = coachRepository.save(Coach.builder()
                .phone("9800090002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Detail Client").phone("9133331111")
                .status(Client.Status.ACTIVE).build());
        rivalClient = clientRepository.save(Client.builder()
                .coachId(rivalCoach.getId()).name("Rival Client").phone("9133331112")
                .status(Client.Status.ACTIVE).build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            UUID id = existing.getId();
            noteRepository.deleteAll(noteRepository.findAll().stream()
                    .filter(n -> n.getCoachId().equals(id)).toList());
            completionRepository.deleteAll(completionRepository.findAll().stream()
                    .filter(c -> c.getCoachId().equals(id)).toList());
            scheduleRepository.deleteAll(scheduleRepository.findAll().stream()
                    .filter(x -> x.getCoachId().equals(id)).toList());
            programAssignmentRepository.deleteAll(programAssignmentRepository.findAll().stream()
                    .filter(a -> a.getCoachId().equals(id)).toList());
            programRepository.findAll().stream().filter(p -> p.getCoachId().equals(id))
                    .forEach(p -> {
                        programDayRepository.deleteAll(
                                programDayRepository.findByProgramIdOrderByDayNumberAsc(p.getId()));
                        programRepository.delete(p);
                    });
            clientRepository.findAllByCoachId(id).forEach(c ->
                    progressLogRepository.deleteAll(progressLogRepository
                            .findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), id)));
            workoutRepository.deleteAll(workoutRepository.findAll().stream()
                    .filter(w -> w.getCoachId().equals(id)).toList());
            clientRepository.deleteAll(clientRepository.findAllByCoachId(id));
            coachRepository.delete(existing);
        });
    }

    private Workout workout(String name) {
        return workoutRepository.save(Workout.builder().coachId(coach.getId()).name(name).build());
    }

    /** Assign a 3-day program starting on the given date. */
    private void assignProgram(Workout w, LocalDate startDate, int... dayNumbers) {
        Program program = programRepository.save(Program.builder()
                .coachId(coach.getId()).name("Plan").weeks(1).durationDays(7).build());
        for (int d : dayNumbers) {
            programDayRepository.save(ProgramDay.builder()
                    .programId(program.getId()).dayNumber(d).workoutId(w.getId()).build());
        }
        programAssignmentRepository.save(ClientProgramAssignment.builder()
                .coachId(coach.getId()).clientId(client.getId()).programId(program.getId())
                .assignedAt(Instant.now()).startDate(startDate).build());
    }

    private void complete(Workout w, LocalDate date) {
        completionRepository.save(ClientWorkoutCompletion.builder()
                .coachId(coach.getId()).clientId(client.getId()).workoutId(w.getId())
                .workoutDate(date).completedAt(Instant.now()).build());
    }

    // ── Training stats ───────────────────────────────────────────────────

    @Test
    void trainingStats_countsPlannedAndCompletedInEachWindow() throws Exception {
        LocalDate today = LocalDate.now();
        Workout w = workout("Push Day");
        // Days 1-3 of a program that started two days ago → planned on
        // today-2, today-1, today (all inside the 7-day window).
        assignProgram(w, today.minusDays(2), 1, 2, 3);
        complete(w, today.minusDays(2));
        complete(w, today.minusDays(1));

        mockMvc.perform(get("/api/v1/clients/{id}/training-stats", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.last7Days.planned").value(3))
                .andExpect(jsonPath("$.data.last7Days.done").value(2))
                .andExpect(jsonPath("$.data.last30Days.planned").value(3))
                .andExpect(jsonPath("$.data.lastWorkout.workoutName").value("Push Day"))
                .andExpect(jsonPath("$.data.lastWorkout.daysAgo").value(1));
    }

    @Test
    void trainingStats_countsAdHocSchedulesAsPlanned() throws Exception {
        LocalDate today = LocalDate.now();
        Workout w = workout("Leg Day");
        scheduleRepository.save(ClientWorkoutSchedule.builder()
                .coachId(coach.getId()).clientId(client.getId()).workoutId(w.getId())
                .scheduledDate(today.minusDays(1)).build());

        mockMvc.perform(get("/api/v1/clients/{id}/training-stats", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.last7Days.planned").value(1))
                .andExpect(jsonPath("$.data.last7Days.done").value(0));
    }

    @Test
    void trainingStats_nextWeekLooksForward() throws Exception {
        LocalDate today = LocalDate.now();
        Workout w = workout("Future Day");
        // Starts tomorrow, days 1 and 2 → both fall in the next-week window.
        assignProgram(w, today.plusDays(1), 1, 2);

        mockMvc.perform(get("/api/v1/clients/{id}/training-stats", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextWeek.planned").value(2))
                .andExpect(jsonPath("$.data.nextWeek.done").value(0))
                .andExpect(jsonPath("$.data.last7Days.planned").value(0));
    }

    @Test
    void trainingStats_withNothingTracked_isAllZeroAndHasNoLastWorkout() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/training-stats", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.last7Days.planned").value(0))
                .andExpect(jsonPath("$.data.last30Days.done").value(0))
                .andExpect(jsonPath("$.data.lastWorkout").doesNotExist());
    }

    @Test
    void trainingStats_forAnotherCoachsClient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/training-stats", rivalClient.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    // ── Activity feed ────────────────────────────────────────────────────

    @Test
    void activity_unionsSourcesNewestFirst_andAlwaysIncludesTheJoinEvent() throws Exception {
        Workout w = workout("Push Day");
        complete(w, LocalDate.now());
        progressLogRepository.save(ProgressLog.builder()
                .coachId(coach.getId()).clientId(client.getId())
                .loggedDate(LocalDate.now()).weightKg(new BigDecimal("72.5")).build());

        mockMvc.perform(get("/api/v1/clients/{id}/activity", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                // progress log + workout completion + joined
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.type=='WORKOUT_DONE')].summary")
                        .value(org.hamcrest.Matchers.hasItem("Completed Push Day")))
                .andExpect(jsonPath("$.data[?(@.type=='PROGRESS_LOG')].summary")
                        .value(org.hamcrest.Matchers.hasItem("Logged weight 72.5 kg")))
                .andExpect(jsonPath("$.data[?(@.type=='CLIENT_JOINED')]")
                        .value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void activity_forANewClient_isJustTheJoinEvent() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/activity", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("CLIENT_JOINED"));
    }

    @Test
    void activity_forAnotherCoachsClient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/activity", rivalClient.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    // ── Notes ────────────────────────────────────────────────────────────

    @Test
    void notes_createListUpdateDelete() throws Exception {
        String created = mockMvc.perform(post("/api/v1/clients/{id}/notes", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "  Prefers morning sessions  "))))
                .andExpect(status().isCreated())
                // Whitespace is trimmed on the way in.
                .andExpect(jsonPath("$.data.body").value("Prefers morning sessions"))
                .andReturn().getResponse().getContentAsString();
        UUID noteId = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/clients/{id}/notes", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(put("/api/v1/clients/{id}/notes/{noteId}", client.getId(), noteId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Prefers evenings now"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("Prefers evenings now"));

        mockMvc.perform(delete("/api/v1/clients/{id}/notes/{noteId}", client.getId(), noteId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients/{id}/notes", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void notes_blankBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/notes", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "   "))))
                .andExpect(status().isBadRequest());
    }

    /** A note id must be reachable only through the client it belongs to. */
    @Test
    void notes_addressedThroughTheWrongClient_returns404() throws Exception {
        Client other = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Other").phone("9133331199")
                .status(Client.Status.ACTIVE).build());

        String created = mockMvc.perform(post("/api/v1/clients/{id}/notes", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Belongs to client one"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID noteId = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

        mockMvc.perform(put("/api/v1/clients/{id}/notes/{noteId}", other.getId(), noteId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Sneaky edit"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void notes_forAnotherCoachsClient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/notes", rivalClient.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void notes_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{id}/notes", client.getId()))
                .andExpect(status().isUnauthorized());
    }
}
