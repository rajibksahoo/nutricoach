package com.nutricoach.common.security;

import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final CoachRepository coachRepository;

    /** For COACH-role requests: resolves coachId via DB lookup by phone. */
    public UUID getCurrentCoachId() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return coachRepository.findByPhone(phone)
                .orElseThrow(() -> NutriCoachException.unauthorized("Authenticated coach not found"))
                .getId();
    }

    /**
     * For CLIENT-role requests: returns clientId from the JWT claims attached
     * to Authentication.details by JwtAuthenticationFilter.
     */
    public UUID getCurrentClientId() {
        return UUID.fromString(getDetails().get("clientId"));
    }

    /**
     * Returns coachId from JWT claims — works for both COACH and CLIENT tokens
     * since both carry a coachId claim.
     */
    public UUID getCurrentCoachIdFromToken() {
        return UUID.fromString(getDetails().get("coachId"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getDetails() instanceof Map)) {
            throw NutriCoachException.unauthorized("Missing token claims");
        }
        return (Map<String, String>) auth.getDetails();
    }
}
