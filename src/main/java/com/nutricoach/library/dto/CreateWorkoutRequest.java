package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateWorkoutRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Positive Integer estimatedDurationMinutes,
        List<String> tags) {}
