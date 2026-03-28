package com.nutricoach.auth.dto;

import com.nutricoach.coach.entity.Coach;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID coachId,
        String phone,
        String name,
        Coach.SubscriptionTier subscriptionTier,
        Coach.SubscriptionStatus subscriptionStatus,
        boolean isNewCoach
) {}
