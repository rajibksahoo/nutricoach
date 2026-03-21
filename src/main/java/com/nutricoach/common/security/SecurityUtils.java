package com.nutricoach.common.security;

import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final CoachRepository coachRepository;

    public UUID getCurrentCoachId() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return coachRepository.findByPhone(phone)
                .orElseThrow(() -> NutriCoachException.unauthorized("Authenticated coach not found"))
                .getId();
    }
}
