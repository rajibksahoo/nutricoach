package com.nutricoach.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.progress.entity.ProgressLog;
import com.nutricoach.progress.entity.ProgressPhoto;
import com.nutricoach.progress.repository.ProgressLogRepository;
import com.nutricoach.progress.repository.ProgressPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProgressPhotoIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired ProgressPhotoRepository progressPhotoRepository;
    @Autowired JwtService jwtService;

    private String jwt;
    private Coach coach;
    private Client client;
    private ProgressLog log;

    private static final String BASE = "/api/v1/clients/{clientId}/progress/{logId}/photos";

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9710000001").ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId())
                        .forEach(l -> progressPhotoRepository.deleteAll(
                                progressPhotoRepository.findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(
                                        existing.getId(), l.getId())));
                progressLogRepository.deleteAll(
                        progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId()));
            });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9710000001").name("Photo Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone("9710000002").name("Photo Client")
                .status(Client.Status.ACTIVE).build());

        log = progressLogRepository.save(ProgressLog.builder()
                .coachId(coach.getId()).clientId(client.getId())
                .loggedDate(LocalDate.now())
                .weightKg(new BigDecimal("72.5"))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    @Test
    void initiateUpload_validRequest_returns201WithUploadUrl() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE, client.getId(), log.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "photoType", "FRONT",
                                "contentType", "image/jpeg"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.photoId").isNotEmpty())
                .andExpect(jsonPath("$.data.photoType").value("FRONT"))
                .andReturn();

        // Verify photo record was persisted
        String body = result.getResponse().getContentAsString();
        String photoId = objectMapper.readTree(body).path("data").path("photoId").asText();
        assertThat(progressPhotoRepository.findById(UUID.fromString(photoId))).isPresent();
    }

    @Test
    void initiateUpload_invalidPhotoType_returns400() throws Exception {
        mockMvc.perform(post(BASE, client.getId(), log.getId())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "photoType", "INVALID",
                                "contentType", "image/jpeg"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void initiateUpload_wrongLog_returns404() throws Exception {
        mockMvc.perform(post(BASE, client.getId(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "photoType", "SIDE",
                                "contentType", "image/jpeg"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void initiateUpload_noToken_returns401() throws Exception {
        mockMvc.perform(post(BASE, client.getId(), log.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "photoType", "BACK",
                                "contentType", "image/jpeg"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listPhotos_returnsExistingPhotos() throws Exception {
        // Create a photo record directly
        progressPhotoRepository.save(ProgressPhoto.builder()
                .coachId(coach.getId()).progressLogId(log.getId())
                .s3Key("photos/test/test/front-abc.jpg")
                .photoType(ProgressPhoto.PhotoType.FRONT)
                .build());

        mockMvc.perform(get(BASE, client.getId(), log.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].photoType").value("FRONT"))
                .andExpect(jsonPath("$.data[0].downloadUrl").isNotEmpty());
    }

    @Test
    void deletePhoto_removesRecord() throws Exception {
        ProgressPhoto photo = progressPhotoRepository.save(ProgressPhoto.builder()
                .coachId(coach.getId()).progressLogId(log.getId())
                .s3Key("photos/test/test/back-abc.jpg")
                .photoType(ProgressPhoto.PhotoType.BACK)
                .build());

        mockMvc.perform(delete(BASE + "/{photoId}", client.getId(), log.getId(), photo.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(progressPhotoRepository.findById(photo.getId())).isEmpty();
    }

    @Test
    void deletePhoto_wrongCoach_returns404() throws Exception {
        Coach other = coachRepository.save(Coach.builder()
                .phone("9710000099").name("Other Coach")
                .trialEndsAt(Instant.now().plusSeconds(86400L)).build());
        String otherJwt = jwtService.generateToken(other.getPhone(), other.getId(), "ROLE_COACH");

        ProgressPhoto photo = progressPhotoRepository.save(ProgressPhoto.builder()
                .coachId(coach.getId()).progressLogId(log.getId())
                .s3Key("photos/test/test/side-abc.jpg")
                .photoType(ProgressPhoto.PhotoType.SIDE)
                .build());

        mockMvc.perform(delete(BASE + "/{photoId}", client.getId(), log.getId(), photo.getId())
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isNotFound());

        coachRepository.delete(other);
    }
}
