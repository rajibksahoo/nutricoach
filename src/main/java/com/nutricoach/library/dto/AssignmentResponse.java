package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID clientId,
        UUID workoutId,
        Instant assignedAt,
        String notes) {}
