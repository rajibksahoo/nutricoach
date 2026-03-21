package com.nutricoach.coach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CoachProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9500000001").ifPresent(existing -> {
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9500000001")
                .name("Rajib Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void getProfile_returnsCoachData() throws Exception {
        mockMvc.perform(get("/api/v1/coach/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value("9500000001"))
                .andExpect(jsonPath("$.data.name").value("Rajib Coach"))
                .andExpect(jsonPath("$.data.subscriptionTier").value("STARTER"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("TRIAL"));
    }

    @Test
    void getProfile_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/coach/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_patchesOnlyProvidedFields() throws Exception {
        mockMvc.perform(put("/api/v1/coach/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Rajib Updated",
                                "businessName", "FitLife Nutrition"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rajib Updated"))
                .andExpect(jsonPath("$.data.businessName").value("FitLife Nutrition"))
                .andExpect(jsonPath("$.data.phone").value("9500000001")); // unchanged
    }

    @Test
    void updateProfile_invalidGstin_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/coach/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gstin", "INVALID"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateProfile_validGstin_saves() throws Exception {
        mockMvc.perform(put("/api/v1/coach/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "gstin", "27AAPFU0939F1ZV"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gstin").value("27AAPFU0939F1ZV"));
    }

    @Test
    void updateProfile_emptyBody_returnsCurrentProfile() throws Exception {
        mockMvc.perform(put("/api/v1/coach/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rajib Coach")); // unchanged
    }
}
