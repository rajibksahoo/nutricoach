package com.nutricoach.library.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PatchSectionExerciseRequest(
        @PositiveOrZero Integer position,
        Integer sets,
        Integer reps,
        Integer durationSeconds,
        Integer restSeconds,
        @Size(max = 40) String weight,
        String notes) {}
