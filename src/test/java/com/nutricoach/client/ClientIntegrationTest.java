package com.nutricoach.client;

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

class ClientIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        // Create a coach directly (bypassing OTP for test isolation)
        coachRepository.findByPhone("9000000001").ifPresent(existing -> {
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9000000001")
                .name("Test Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void createClient_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "9111111111",
                                "name", "Priya Sharma"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value("9111111111"))
                .andExpect(jsonPath("$.data.name").value("Priya Sharma"))
                .andExpect(jsonPath("$.data.status").value("ONBOARDING"));
    }

    @Test
    void createClient_missingPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "No Phone"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createClient_duplicatePhone_returns409() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("phone", "9222222222", "name", "Duplicate"));

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void listClients_withStatusFilter_returnsFiltered() throws Exception {
        // Create a client
        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "9333333333", "name", "Filter Test"))))
                .andExpect(status().isCreated());

        // List all
        mockMvc.perform(get("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Filter by ONBOARDING
        mockMvc.perform(get("/api/v1/clients?status=ONBOARDING")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ONBOARDING"));
    }

    @Test
    void listClients_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateClient_patchesFieldsOnly() throws Exception {
        // Create client
        var result = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "9444444444", "name", "Patch Me"))))
                .andExpect(status().isCreated())
                .andReturn();

        String clientId = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asText();

        // Patch name only
        mockMvc.perform(put("/api/v1/clients/" + clientId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value("9444444444")); // unchanged
    }

    @Test
    void deleteClient_softDeletes_thenNotFoundOnGet() throws Exception {
        // Create
        var result = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "9555555555", "name", "Delete Me"))))
                .andExpect(status().isCreated())
                .andReturn();

        String clientId = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asText();

        // Delete
        mockMvc.perform(delete("/api/v1/clients/" + clientId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // GET should 404
        mockMvc.perform(get("/api/v1/clients/" + clientId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}
