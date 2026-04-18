package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutResponse(
        UUID id,
        String name,
        String description,
        Integer estimatedDurationMinutes,
        List<WorkoutSectionResponse> sections,
        Instant createdAt,
        Instant updatedAt) {}
