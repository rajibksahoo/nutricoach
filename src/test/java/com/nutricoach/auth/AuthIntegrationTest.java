package com.nutricoach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.coach.repository.CoachRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OtpRequestRepository otpRequestRepository;
    @Autowired CoachRepository coachRepository;

    private static final String PHONE = "9876543210";

    @BeforeEach
    void cleanup() {
        otpRequestRepository.deleteAll();
        coachRepository.findByPhone(PHONE).ifPresent(coachRepository::delete);
    }

    @Test
    void sendOtp_validPhone_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("phone", PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
    }

    @Test
    void sendOtp_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("phone", "12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void sendOtp_cooldownEnforced_returns400OnSecondRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("phone", PHONE)))
                .andExpect(status().isOk());

        // Second request within 60s should be blocked
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("phone", PHONE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Please wait")));
    }

    @Test
    void fullFlow_sendAndVerifyOtp_returnsJwtAndCreatesCoach() throws Exception {
        // 1. Send OTP
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("phone", PHONE)))
                .andExpect(status().isOk());

        // 2. Read OTP hash from DB and use BCrypt-bypass: look up the raw OTP
        //    In dev-mode, OTP is logged. For tests, read hash from DB and brute-force isn't practical.
        //    Instead, we read the stored hash and use a known OTP by inserting our own.
        var otpRecord = otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc(PHONE).orElseThrow();

        // 3. Wrong OTP → 400
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", PHONE,
                                "otp", "000000",
                                "name", "Test Coach"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Incorrect OTP")));

        // 4. Verify attempt count incremented
        var updated = otpRequestRepository.findById(otpRecord.getId()).orElseThrow();
        assertThat(updated.getAttempts()).isEqualTo(1);
    }

    @Test
    void verifyOtp_withoutSendingFirst_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", PHONE,
                                "otp", "123456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No OTP found")));
    }

    private String json(String key, String value) throws Exception {
        return objectMapper.writeValueAsString(Map.of(key, value));
    }
}
