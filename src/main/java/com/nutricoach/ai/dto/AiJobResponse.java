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
        UUID generatedMealPlanId
) {}
