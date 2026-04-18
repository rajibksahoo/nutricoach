package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 50) String muscleGroup,
        @Size(max = 80) String equipment,
        @Size(max = 500) String videoUrl,
        String notes) {}
