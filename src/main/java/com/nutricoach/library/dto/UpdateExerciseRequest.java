package com.nutricoach.library.dto;

import com.nutricoach.library.entity.Exercise;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateExerciseRequest(
        @Size(max = 150) String name,
        String description,
        @Size(max = 50) String muscleGroup,
        @Size(max = 80) String equipment,
        @Size(max = 500) String videoUrl,
        String notes,
        Exercise.Category category,
        @Size(max = 60) String movementPattern,
        List<String> tags) {}
