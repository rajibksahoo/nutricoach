package com.nutricoach.progress;

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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GET /api/v1/clients/{clientId}/progress/photos — every photo for one client.
 *
 * <p>Photos belong to progress logs, not to clients, so the interesting part is
 * the join: the right photos, in log-date order, carrying their log's date, and
 * never another coach's.
 */
class ClientProgressPhotosIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired ProgressPhotoRepository progressPhotoRepository;
    @Autowired JwtService jwtService;

    private static final String URL = "/api/v1/clients/{clientId}/progress/photos";

    private String jwt;
    private Coach coach;
    private Client client;
    private Client otherClientSameCoach;
    private Client rivalClient;

    @BeforeEach
    void setup() {
        cleanup("9710010001");
        cleanup("9710010002");

        coach = coachRepository.save(Coach.builder()
                .phone("9710010001").name("Photo Grid Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());
        Coach rivalCoach = coachRepository.save(Coach.builder()
                .phone("9710010002").name("Rival Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)).build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone("9710011111").name("Grid Client")
                .status(Client.Status.ACTIVE).build());
        otherClientSameCoach = clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone("9710011112").name("Other Client")
                .status(Client.Status.ACTIVE).build());
        rivalClient = clientRepository.save(Client.builder()
                .coachId(rivalCoach.getId()).phone("9710011113").name("Rival Client")
                .status(Client.Status.ACTIVE).build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    private void cleanup(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c ->
                    progressLogRepository
                            .findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId())
                            .forEach(l -> {
                                progressPhotoRepository.deleteAll(progressPhotoRepository
                                        .findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(existing.getId(), l.getId()));
                                progressLogRepository.delete(l);
                            }));
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });
    }

    private ProgressLog logOn(Client c, LocalDate date) {
        return progressLogRepository.save(ProgressLog.builder()
                .coachId(c.getCoachId()).clientId(c.getId()).loggedDate(date).build());
    }

    private void photoOn(ProgressLog log, ProgressPhoto.PhotoType type) {
        progressPhotoRepository.save(ProgressPhoto.builder()
                .coachId(log.getCoachId()).progressLogId(log.getId())
                .s3Key("coaches/%s/%s-%s.jpg".formatted(log.getCoachId(), log.getId(), type))
                .photoType(type).build());
    }

    @Test
    void getPhotos_returnsEveryPhotoAcrossLogs_newestLogFirst() throws Exception {
        photoOn(logOn(client, LocalDate.of(2026, 9, 1)), ProgressPhoto.PhotoType.FRONT);
        ProgressLog recent = logOn(client, LocalDate.of(2026, 9, 15));
        photoOn(recent, ProgressPhoto.PhotoType.FRONT);
        photoOn(recent, ProgressPhoto.PhotoType.SIDE);

        mockMvc.perform(get(URL, client.getId()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                // Newest log first; the older log's photo trails.
                .andExpect(jsonPath("$.data[0].loggedDate").value("2026-09-15"))
                .andExpect(jsonPath("$.data[1].loggedDate").value("2026-09-15"))
                .andExpect(jsonPath("$.data[2].loggedDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data[0].downloadUrl").isNotEmpty())
                .andExpect(jsonPath("$.data[0].photoType").value("FRONT"));
    }

    @Test
    void getPhotos_withNoPhotos_returnsEmptyList() throws Exception {
        logOn(client, LocalDate.of(2026, 9, 1));

        mockMvc.perform(get(URL, client.getId()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /** The join must filter by client, not just by coach. */
    @Test
    void getPhotos_excludesAnotherClientOfTheSameCoach() throws Exception {
        photoOn(logOn(client, LocalDate.of(2026, 9, 1)), ProgressPhoto.PhotoType.FRONT);
        photoOn(logOn(otherClientSameCoach, LocalDate.of(2026, 9, 2)), ProgressPhoto.PhotoType.BACK);

        mockMvc.perform(get(URL, client.getId()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].photoType").value("FRONT"));
    }

    @Test
    void getPhotos_forAnotherCoachsClient_returns404() throws Exception {
        mockMvc.perform(get(URL, rivalClient.getId()).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPhotos_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URL, client.getId()))
                .andExpect(status().isUnauthorized());
    }
}
