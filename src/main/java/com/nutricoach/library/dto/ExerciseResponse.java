package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        String description,
        String muscleGroup,
        String equipment,
        String videoUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
