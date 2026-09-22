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
 * Enforces per-tier client limits and per-tier feature access.
 *
 * Limits:
 *   TRIAL        →  5 active clients
 *   STARTER      → 25 active clients
 *   PROFESSIONAL → 100 active clients
 *   ENTERPRISE   → unlimited
 *
 * Features:
 *   AI meal-plan generation → TRIAL, PROFESSIONAL, ENTERPRISE (not STARTER)
 *
 * Both the caps and the feature rules are mirrored in the frontend's
 * {@code lib/plans.ts} catalogue, which is what the pricing page renders. Change
 * one and change the other, or the product sells something it does not deliver.
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

    /**
     * AI meal-plan generation is a paid feature: it carries a real per-call cost
     * (OpenAI) and it is the reason to buy PROFESSIONAL over STARTER.
     *
     * <p>TRIAL keeps access on purpose — a coach who never sees the generator has
     * no reason to upgrade for it, and trial is already capped at 5 clients, so the
     * cost exposure is small.
     */
    @Transactional(readOnly = true)
    public void requireAiMealPlans(UUID coachId) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Coach not found"));

        if (!hasAiMealPlans(coach)) {
            throw NutriCoachException.paymentRequired(
                    "AI meal plan generation is available on the Professional plan and above. "
                    + "Please upgrade to use it.");
        }
    }

    /**
     * Whether a coach's current tier/status includes AI meal-plan generation.
     * Public so read-only callers report the same answer this gate enforces.
     */
    public boolean hasAiMealPlans(Coach coach) {
        if (coach.getSubscriptionStatus() == Coach.SubscriptionStatus.TRIAL) {
            return true;
        }
        return switch (coach.getSubscriptionTier()) {
            case STARTER                   -> false;
            case PROFESSIONAL, ENTERPRISE  -> true;
        };
    }

    /**
     * Client cap for a coach's current tier/status. {@link Integer#MAX_VALUE} means unlimited.
     * Public so read-only callers (e.g. the dashboard) report the same limit this gate enforces.
     */
    public int clientLimitFor(Coach coach) {
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
