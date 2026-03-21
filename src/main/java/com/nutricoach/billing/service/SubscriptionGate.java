package com.nutricoach.billing.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enforces per-tier client limits.
 *
 * Limits:
 *   TRIAL        →  5 active clients
 *   STARTER      → 25 active clients
 *   PROFESSIONAL → 100 active clients
 *   ENTERPRISE   → unlimited
 */
@Service
@RequiredArgsConstructor
public class SubscriptionGate {

    private final CoachRepository coachRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public void requireClientSlot(UUID coachId) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Coach not found"));

        int limit = clientLimitFor(coach);
        if (limit == Integer.MAX_VALUE) return;

        long active = clientRepository.countByCoachIdAndDeletedAtIsNull(coachId);
        if (active >= limit) {
            String tierName = coach.getSubscriptionTier().name();
            throw NutriCoachException.paymentRequired(
                    "Client limit reached for " + tierName + " plan (" + limit + " clients). " +
                    "Please upgrade to add more clients.");
        }
    }

    private int clientLimitFor(Coach coach) {
        // During trial, use TRIAL limits regardless of tier
        if (coach.getSubscriptionStatus() == Coach.SubscriptionStatus.TRIAL) {
            return 5;
        }
        return switch (coach.getSubscriptionTier()) {
            case STARTER      -> 25;
            case PROFESSIONAL -> 100;
            case ENTERPRISE   -> Integer.MAX_VALUE;
        };
    }
}
