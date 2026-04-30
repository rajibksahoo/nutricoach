package com.nutricoach.library.dto;

import com.nutricoach.library.entity.Exercise;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        String description,
        String muscleGroup,
        String equipment,
        String videoUrl,
        String notes,
        Exercise.Category category,
        String movementPattern,
        List<String> tags,
        boolean custom,
        Instant createdAt,
        Instant updatedAt) {}
