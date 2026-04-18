package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkoutSummaryResponse(
        UUID id,
        String name,
        String description,
        Integer estimatedDurationMinutes,
        Instant createdAt,
        Instant updatedAt) {}
