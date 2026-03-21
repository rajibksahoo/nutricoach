package com.nutricoach.coach.dto;

import com.nutricoach.coach.entity.Coach;

import java.time.Instant;
import java.util.UUID;

public record CoachResponse(
        UUID id,
        String phone,
        String name,
        String email,
        String businessName,
        String gstin,
        Coach.SubscriptionTier subscriptionTier,
        Coach.SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt,
        Instant createdAt
) {}
