package com.nutricoach.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.repository.CheckInRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Editing and removing a check-in.
 *
 * <p>A coach could create and list check-ins but nothing more, so `coachNotes`
 * was only settable at creation — meaning a check-in the *client* submitted
 * could never be replied to. The portal has always rendered the reply; there
 * was simply no way to write one.
 */
class CheckInLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired CheckInRepository checkInRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private Client otherClient;
    private MealPlan plan;

    @BeforeEach
    void setup() {
        cleanup("9820020001");
        cleanup("9820020002");

        coach = coachRepository.save(Coach.builder()
                .phone("9820020001").name("Check-in Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        Coach rivalCoach = coachRepository.save(Coach.builder()
                .phone("9820020002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Check-in Client").phone("9823330001")
                .status(Client.Status.ACTIVE).build());
        otherClient = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Other Client").phone("9823330002")
                .status(Client.Status.ACTIVE).build());
        clientRepository.save(Client.builder()
                .coachId(rivalCoach.getId()).name("Rival Client").phone("9823330003")
                .status(Client.Status.ACTIVE).build());

        plan = mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId()).clientId(client.getId()).name("Plan")
                .status(MealPlan.Status.ACTIVE).build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            UUID id = existing.getId();
            checkInRepository.deleteAll(checkInRepository.findAll().stream()
                    .filter(c -> c.getCoachId().equals(id)).toList());
            mealPlanRepository.deleteAll(mealPlanRepository.findAll().stream()
                    .filter(p -> p.getCoachId().equals(id)).toList());
            clientRepository.deleteAll(clientRepository.findAllByCoachId(id));
            coachRepository.delete(existing);
        });
    }

    /** A check-in as the client would have filed it: no coach reply yet. */
    private CheckIn clientSubmitted(LocalDate date) {
        return checkInRepository.save(CheckIn.builder()
                .coachId(coach.getId()).clientId(client.getId()).mealPlanId(plan.getId())
                .checkInDate(date).adherencePercent(80)
                .clientNotes("Struggled with dinners this week").build());
    }

    private Map<String, Object> body(Map<String, Object> fields) {
        return new HashMap<>(fields);
    }

    @Test
    void update_letsTheCoachReplyToAClientCheckIn() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 10));

        mockMvc.perform(put("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(Map.of("coachNotes", "Let's plan dinners on Sunday.")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coachNotes").value("Let's plan dinners on Sunday."))
                // Replying must not disturb what the client wrote.
                .andExpect(jsonPath("$.data.clientNotes").value("Struggled with dinners this week"))
                .andExpect(jsonPath("$.data.adherencePercent").value(80));
    }

    @Test
    void update_withoutCoachNotes_leavesTheExistingReply() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 11));
        ci.setCoachNotes("Original reply");
        checkInRepository.save(ci);

        mockMvc.perform(put("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(Map.of("adherencePercent", 95)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adherencePercent").value(95))
                .andExpect(jsonPath("$.data.coachNotes").value("Original reply"));
    }

    @Test
    void update_rejectsAnOutOfRangeAdherence() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 12));

        mockMvc.perform(put("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(Map.of("adherencePercent", 150)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_removesItFromHistory() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 13));

        mockMvc.perform(delete("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients/{clientId}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // Soft, not hard — the row is still there, flagged.
        assertThat(checkInRepository.findById(ci.getId()).orElseThrow().getDeletedAt()).isNotNull();
    }

    /** A removed check-in must free its date, or the coach cannot re-file it. */
    @Test
    void delete_freesTheDateForANewCheckIn() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 14));
        mockMvc.perform(delete("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/clients/{clientId}/check-ins", client.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(Map.of(
                                "mealPlanId", plan.getId().toString(),
                                "checkInDate", "2026-09-14",
                                "adherencePercent", 70)))))
                .andExpect(status().isCreated());
    }

    /** A removed check-in must not appear in the client's activity feed either. */
    @Test
    void delete_removesItFromTheActivityFeed() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 15));

        mockMvc.perform(get("/api/v1/clients/{id}/activity", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(jsonPath("$.data[?(@.type=='CHECK_IN')]")
                        .value(org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(delete("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clients/{id}/activity", client.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(jsonPath("$.data[?(@.type=='CHECK_IN')]")
                        .value(org.hamcrest.Matchers.hasSize(0)));
    }

    /** A check-in id must be reachable only through the client it belongs to. */
    @Test
    void update_addressedThroughTheWrongClient_returns404() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 16));

        mockMvc.perform(put("/api/v1/clients/{clientId}/check-ins/{id}", otherClient.getId(), ci.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(Map.of("coachNotes", "Sneaky")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_unknownCheckIn_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(Map.of("coachNotes", "Hello")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withoutToken_returns401() throws Exception {
        CheckIn ci = clientSubmitted(LocalDate.of(2026, 9, 17));

        mockMvc.perform(put("/api/v1/clients/{clientId}/check-ins/{id}", client.getId(), ci.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(Map.of("coachNotes", "Hello")))))
                .andExpect(status().isUnauthorized());
    }
}
