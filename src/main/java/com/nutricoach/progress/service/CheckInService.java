package com.nutricoach.progress.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.dto.CheckInResponse;
import com.nutricoach.progress.dto.CreateCheckInRequest;
import com.nutricoach.progress.dto.UpdateCheckInRequest;
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.mapper.ProgressMapper;
import com.nutricoach.progress.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final ClientRepository clientRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ProgressMapper progressMapper;

    @Transactional
    public CheckInResponse create(UUID clientId, UUID coachId, CreateCheckInRequest req) {
        requireClientOwned(clientId, coachId);

        mealPlanRepository.findByIdAndCoachIdAndDeletedAtIsNull(req.mealPlanId(), coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Meal plan not found"));

        if (checkInRepository.existsByClientIdAndCoachIdAndCheckInDateAndDeletedAtIsNull(clientId, coachId, req.checkInDate())) {
            throw NutriCoachException.conflict("A check-in already exists for this date");
        }

        CheckIn checkIn = CheckIn.builder()
                .clientId(clientId)
                .coachId(coachId)
                .mealPlanId(req.mealPlanId())
                .checkInDate(req.checkInDate())
                .adherencePercent(req.adherencePercent())
                .clientNotes(req.clientNotes())
                .coachNotes(req.coachNotes())
                .build();

        return progressMapper.toResponse(checkInRepository.save(checkIn));
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> getHistory(UUID clientId, UUID coachId) {
        requireClientOwned(clientId, coachId);
        return checkInRepository
                .findByClientIdAndCoachIdAndDeletedAtIsNullOrderByCheckInDateDesc(clientId, coachId)
                .stream().map(progressMapper::toResponse).toList();
    }

    /**
     * Edit a check-in, including the coach's reply.
     *
     * <p>This is what makes a client-submitted check-in a conversation rather
     * than a write-only void: {@code coachNotes} could only be set at creation,
     * so a coach had no way to respond to one the client had filed. The portal
     * already renders the reply.
     */
    @Transactional
    public CheckInResponse update(UUID clientId, UUID checkInId, UUID coachId, UpdateCheckInRequest req) {
        CheckIn checkIn = require(clientId, checkInId, coachId);

        if (req.adherencePercent() != null) checkIn.setAdherencePercent(req.adherencePercent());
        if (req.clientNotes() != null) checkIn.setClientNotes(req.clientNotes());
        if (req.coachNotes() != null) checkIn.setCoachNotes(req.coachNotes());

        return progressMapper.toResponse(checkInRepository.save(checkIn));
    }

    @Transactional
    public void delete(UUID clientId, UUID checkInId, UUID coachId) {
        CheckIn checkIn = require(clientId, checkInId, coachId);
        checkIn.setDeletedAt(Instant.now());
        checkInRepository.save(checkIn);
    }

    /**
     * A check-in is addressed through its client, so it must actually belong to
     * that client — otherwise one coach's check-in id could be edited under a
     * different client of theirs.
     */
    private CheckIn require(UUID clientId, UUID checkInId, UUID coachId) {
        requireClientOwned(clientId, coachId);
        CheckIn checkIn = checkInRepository.findByIdAndCoachIdAndDeletedAtIsNull(checkInId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Check-in not found"));
        if (!checkIn.getClientId().equals(clientId)) {
            throw NutriCoachException.notFound("Check-in not found");
        }
        return checkIn;
    }

    private void requireClientOwned(UUID clientId, UUID coachId) {
        if (!clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId).isPresent()) {
            throw NutriCoachException.notFound("Client not found");
        }
    }
}
