package com.nutricoach.library.dto;

import com.nutricoach.library.entity.WorkoutSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkoutSectionRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull WorkoutSection.Type sectionType,
        String description) {}
