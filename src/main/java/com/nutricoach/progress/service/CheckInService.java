package com.nutricoach.progress.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.dto.CheckInResponse;
import com.nutricoach.progress.dto.CreateCheckInRequest;
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.mapper.ProgressMapper;
import com.nutricoach.progress.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (checkInRepository.existsByClientIdAndCoachIdAndCheckInDate(clientId, coachId, req.checkInDate())) {
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
                .findByClientIdAndCoachIdOrderByCheckInDateDesc(clientId, coachId)
                .stream().map(progressMapper::toResponse).toList();
    }

    private void requireClientOwned(UUID clientId, UUID coachId) {
        if (!clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId).isPresent()) {
            throw NutriCoachException.notFound("Client not found");
        }
    }
}
