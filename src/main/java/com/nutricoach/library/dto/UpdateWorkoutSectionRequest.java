package com.nutricoach.library.dto;

import com.nutricoach.library.entity.WorkoutSection;
import jakarta.validation.constraints.Size;

public record UpdateWorkoutSectionRequest(
        @Size(max = 150) String name,
        WorkoutSection.Type sectionType,
        String description) {}
