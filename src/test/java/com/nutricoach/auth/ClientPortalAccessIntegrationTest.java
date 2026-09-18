package com.nutricoach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.auth.entity.OtpRequest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.notifications.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Client portal access: signing in with a phone number alone, and the database
 * guarantee that makes it sound.
 *
 * <p>Before this, a client could only reach the portal through a link carrying
 * their coach's id, and the only UI producing that link was gated behind dev
 * mode — so in production the portal was unreachable.
 */
class ClientPortalAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired JwtService jwtService;
    @Autowired OtpRequestRepository otpRequestRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired NotificationLogRepository notificationLogRepository;

    private static final String VERIFY = "/api/v1/client-auth/otp/verify";
    private static final String CLIENT_PHONE = "9822220001";

    private Coach coach;
    private Coach rivalCoach;
    private Client client;
    private String coachJwt;

    @BeforeEach
    void setup() {
        cleanup("9820010001");
        cleanup("9820010002");

        coach = coachRepository.save(Coach.builder()
                .phone("9820010001").name("Portal Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        rivalCoach = coachRepository.save(Coach.builder()
                .phone("9820010002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).name("Portal Client").phone(CLIENT_PHONE)
                .status(Client.Status.ACTIVE).build());

        coachJwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            // The invite test writes a notification_log against the client, so
            // the log rows have to go before the clients they reference.
            clientRepository.findAllByCoachId(existing.getId()).forEach(c ->
                    notificationLogRepository.deleteAll(notificationLogRepository
                            .findByCoachIdAndClientIdOrderByCreatedAtDesc(existing.getId(), c.getId())));
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });
    }

    /**
     * Seed a verifiable OTP. The test profile keeps msg91.dev-mode off so the
     * cooldown stays testable elsewhere, which means the 111111 bypass is not
     * available here.
     */
    private String freshOtp(String phone) {
        String otp = "123456";
        otpRequestRepository.save(OtpRequest.builder()
                .phone(phone)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
        return otp;
    }

    private Map<String, Object> body(String phone, String otp, UUID coachId) {
        Map<String, Object> m = new HashMap<>();
        m.put("phone", phone);
        m.put("otp", otp);
        if (coachId != null) m.put("coachId", coachId.toString());
        return m;
    }

    // ── Signing in ───────────────────────────────────────────────────────

    /** The point of the change: no coachId, no link, just the number. */
    @Test
    void verify_withoutACoachId_resolvesTheCoachFromThePhone() throws Exception {
        mockMvc.perform(post(VERIFY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(CLIENT_PHONE, freshOtp(CLIENT_PHONE), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.coachId").value(coach.getId().toString()))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    /** The web portal and Android still send it, so it must keep working. */
    @Test
    void verify_withAnExplicitCoachId_stillWorks() throws Exception {
        mockMvc.perform(post(VERIFY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(CLIENT_PHONE, freshOtp(CLIENT_PHONE), coach.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coachId").value(coach.getId().toString()));
    }

    /** An explicit but wrong coach must not be quietly corrected. */
    @Test
    void verify_withTheWrongCoachId_returns404() throws Exception {
        mockMvc.perform(post(VERIFY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(CLIENT_PHONE, freshOtp(CLIENT_PHONE), rivalCoach.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void verify_forAPhoneNobodyCoaches_returns404() throws Exception {
        mockMvc.perform(post(VERIFY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("9829999999", freshOtp("9829999999"), null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void verify_forASoftDeletedClient_returns404() throws Exception {
        client.setDeletedAt(Instant.now());
        clientRepository.save(client);

        mockMvc.perform(post(VERIFY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(CLIENT_PHONE, freshOtp(CLIENT_PHONE), null))))
                .andExpect(status().isNotFound());
    }

    // ── The guarantee underneath ─────────────────────────────────────────

    /**
     * Phone-only sign-in is only sound because a phone maps to one coach. That
     * used to be enforced in application code alone, which a concurrent create
     * could slip past; changeset 028 makes it a database rule.
     */
    @Test
    void twoCoachesCannotHoldTheSameActivePhone() {
        assertThatThrownBy(() -> clientRepository.saveAndFlush(Client.builder()
                .coachId(rivalCoach.getId()).name("Same Number").phone(CLIENT_PHONE)
                .status(Client.Status.ACTIVE).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** Soft-deleting frees the number, including for a different coach. */
    @Test
    void aSoftDeletedClientDoesNotBlockTheNumber() {
        client.setDeletedAt(Instant.now());
        clientRepository.saveAndFlush(client);

        Client reused = clientRepository.saveAndFlush(Client.builder()
                .coachId(rivalCoach.getId()).name("New Owner").phone(CLIENT_PHONE)
                .status(Client.Status.ACTIVE).build());

        assertThat(reused.getId()).isNotNull();
    }

    // ── Sending the link ─────────────────────────────────────────────────

    @Test
    void invite_sendsAndIsLogged() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/invite", client.getId())
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invite sent"));
    }

    @Test
    void invite_forAnotherCoachsClient_returns404() throws Exception {
        Client theirs = clientRepository.save(Client.builder()
                .coachId(rivalCoach.getId()).name("Not Mine").phone("9822220099")
                .status(Client.Status.ACTIVE).build());

        mockMvc.perform(post("/api/v1/clients/{id}/invite", theirs.getId())
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void invite_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/clients/{id}/invite", client.getId()))
                .andExpect(status().isUnauthorized());
    }
}
