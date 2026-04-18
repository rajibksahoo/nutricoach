package com.nutricoach.library.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateWorkoutRequest(
        @Size(max = 150) String name,
        String description,
        @Positive Integer estimatedDurationMinutes) {}
