package com.nutricoach.progress.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.progress.dto.ClientPhotoResponse;
import com.nutricoach.progress.dto.LogProgressRequest;
import com.nutricoach.progress.dto.ProgressLogResponse;
import com.nutricoach.progress.entity.ProgressLog;
import com.nutricoach.progress.entity.ProgressPhoto;
import com.nutricoach.progress.mapper.ProgressMapper;
import com.nutricoach.progress.repository.ProgressLogRepository;
import com.nutricoach.progress.repository.ProgressPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressLogRepository progressLogRepository;
    private final ClientRepository clientRepository;
    private final ProgressMapper progressMapper;
    private final ProgressPhotoRepository progressPhotoRepository;
    private final S3Service s3Service;

    @Transactional
    public ProgressLogResponse log(UUID clientId, UUID coachId, LogProgressRequest req) {
        requireClientOwned(clientId, coachId);

        // Upsert — one log per client per day
        ProgressLog log = progressLogRepository
                .findByClientIdAndCoachIdAndLoggedDate(clientId, coachId, req.loggedDate())
                .orElseGet(() -> ProgressLog.builder()
                        .clientId(clientId)
                        .coachId(coachId)
                        .loggedDate(req.loggedDate())
                        .build());

        if (req.weightKg() != null)        log.setWeightKg(req.weightKg());
        if (req.bodyFatPercent() != null)  log.setBodyFatPercent(req.bodyFatPercent());
        if (req.waistCm() != null)         log.setWaistCm(req.waistCm());
        if (req.chestCm() != null)         log.setChestCm(req.chestCm());
        if (req.hipCm() != null)           log.setHipCm(req.hipCm());
        if (req.adherencePercent() != null) log.setAdherencePercent(req.adherencePercent());
        if (req.notes() != null)           log.setNotes(req.notes());

        return progressMapper.toResponse(progressLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<ProgressLogResponse> getHistory(UUID clientId, UUID coachId) {
        requireClientOwned(clientId, coachId);
        return progressLogRepository
                .findByClientIdAndCoachIdOrderByLoggedDateDesc(clientId, coachId)
                .stream().map(progressMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProgressLogResponse> getChart(UUID clientId, UUID coachId, int days) {
        requireClientOwned(clientId, coachId);
        LocalDate to   = LocalDate.now();
        LocalDate from = to.minusDays(days);
        return progressLogRepository
                .findByClientIdAndCoachIdAndLoggedDateBetweenOrderByLoggedDateAsc(clientId, coachId, from, to)
                .stream().map(progressMapper::toResponse).toList();
    }

    /**
     * Every progress photo for a client, newest log first, each with a
     * pre-signed download URL.
     *
     * <p>Photos hang off progress logs, so before this there was no way to ask
     * "show me this client's photos" without walking every log.
     */
    @Transactional(readOnly = true)
    public List<ClientPhotoResponse> getPhotos(UUID clientId, UUID coachId) {
        requireClientOwned(clientId, coachId);
        return progressPhotoRepository.findByClientIdWithLogDate(clientId, coachId).stream()
                .map(row -> {
                    ProgressPhoto photo = (ProgressPhoto) row[0];
                    return new ClientPhotoResponse(
                            photo.getId(),
                            photo.getProgressLogId(),
                            (LocalDate) row[1],
                            photo.getPhotoType().name(),
                            s3Service.presignDownload(photo.getS3Key()),
                            photo.getCreatedAt());
                })
                .toList();
    }

    private void requireClientOwned(UUID clientId, UUID coachId) {
        if (!clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId).isPresent()) {
            throw NutriCoachException.notFound("Client not found");
        }
    }
}
