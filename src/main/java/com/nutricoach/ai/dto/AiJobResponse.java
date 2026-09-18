package com.nutricoach.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record AiJobResponse(

        UUID id,
        UUID clientId,
        String status,
        String jobType,
        Instant createdAt,
        Instant completedAt,
        String errorMessage,
        UUID generatedMealPlanId,
        /** What the generation actually produced; null until the job completes. */
        Integer dayCount,
        Integer mealCount,
        Integer itemCount,
        /** Items naming a food outside the curated list, so their macros need a look. */
        Integer unmatchedCount
) {}
