package com.nutricoach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Enable the universal 111111 OTP bypass for these tests (same as local dev profile)
@TestPropertySource(properties = "app.msg91.dev-mode=true")
class ClientAuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired OtpRequestRepository otpRequestRepository;
    @Autowired JwtService jwtService;

    private static final String CLIENT_PHONE = "9800000001";
    private static final String UNKNOWN_PHONE = "9800000002";

    private Coach coach;
    private Client client;

    @BeforeEach
    void setup() {
        otpRequestRepository.deleteAll();

        // Clean up clients first (FK), then coach
        coachRepository.findByPhone("9700000001").ifPresent(existing -> {
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9700000001")
                .name("Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone(CLIENT_PHONE)
                .name("Priya Client")
                .status(Client.Status.ACTIVE)
                .build());
    }

    // ── OTP send ─────────────────────────────────────────────────────────────

    @Test
    void sendOtp_validPhone_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/client-auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", CLIENT_PHONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
    }

    @Test
    void sendOtp_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/client-auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── OTP verify ───────────────────────────────────────────────────────────

    @Test
    void verifyOtp_devBypass_validClient_returnsTokenAndClientDetails() throws Exception {
        mockMvc.perform(post("/api/v1/client-auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", CLIENT_PHONE,
                                "otp", "111111",
                                "coachId", coach.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data.coachId").value(coach.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Priya Client"))
                .andExpect(jsonPath("$.data.phone").value(CLIENT_PHONE));
    }

    @Test
    void verifyOtp_phoneNotRegisteredWithCoach_returns404() throws Exception {
        // UNKNOWN_PHONE has never been added as a client of this coach
        mockMvc.perform(post("/api/v1/client-auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", UNKNOWN_PHONE,
                                "otp", "111111",
                                "coachId", coach.getId().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyOtp_wrongCoachId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/client-auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", CLIENT_PHONE,
                                "otp", "111111",
                                "coachId", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyOtp_missingCoachId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/client-auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", CLIENT_PHONE,
                                "otp", "111111"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── Token role isolation ──────────────────────────────────────────────────

    @Test
    void clientToken_cannotAccessCoachEndpoints_returns403() throws Exception {
        String clientJwt = jwtService.generateClientToken(CLIENT_PHONE, client.getId(), coach.getId());

        // /api/v1/clients requires ROLE_COACH — client token must be rejected
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void noToken_protectedEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void coachToken_cannotAccessClientAuthVerify_wrongRole() throws Exception {
        // A coach JWT should not be able to impersonate a client —
        // the coach phone is not registered as a client, so 404 is returned.
        String coachJwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
        mockMvc.perform(post("/api/v1/client-auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + coachJwt)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", coach.getPhone(),
                                "otp", "111111",
                                "coachId", coach.getId().toString()))))
                .andExpect(status().isNotFound());
    }
}
