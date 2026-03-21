package com.nutricoach.coach.service;

import com.nutricoach.coach.dto.CoachResponse;
import com.nutricoach.coach.dto.UpdateCoachRequest;
import com.nutricoach.coach.mapper.CoachMapper;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;

    @Transactional(readOnly = true)
    public CoachResponse getProfile(UUID coachId) {
        return coachMapper.toResponse(
                coachRepository.findById(coachId)
                        .orElseThrow(() -> NutriCoachException.notFound("Coach not found"))
        );
    }

    @Transactional
    public CoachResponse updateProfile(UUID coachId, UpdateCoachRequest req) {
        var coach = coachRepository.findById(coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Coach not found"));

        if (StringUtils.hasText(req.name()))         coach.setName(req.name());
        if (req.email() != null)                      coach.setEmail(req.email());
        if (req.businessName() != null)               coach.setBusinessName(req.businessName());
        if (req.gstin() != null)                      coach.setGstin(req.gstin());

        return coachMapper.toResponse(coachRepository.save(coach));
    }
}
