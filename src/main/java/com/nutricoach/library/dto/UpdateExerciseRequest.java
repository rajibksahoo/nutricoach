package com.nutricoach.library.dto;

import jakarta.validation.constraints.Size;

public record UpdateExerciseRequest(
        @Size(max = 150) String name,
        String description,
        @Size(max = 50) String muscleGroup,
        @Size(max = 80) String equipment,
        @Size(max = 500) String videoUrl,
        String notes) {}
