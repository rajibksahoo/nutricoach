package com.nutricoach.library.dto;

import java.util.UUID;

public record SectionExerciseResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        int position,
        Integer sets,
        Integer reps,
        Integer durationSeconds,
        Integer restSeconds,
        String notes) {}
