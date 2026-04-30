package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddSectionExerciseRequest(
        @NotNull UUID exerciseId,
        Integer position,
        Integer sets,
        Integer reps,
        Integer durationSeconds,
        Integer restSeconds,
        @Size(max = 40) String weight,
        String notes) {}
