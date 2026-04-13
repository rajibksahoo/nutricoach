package com.nutricoach.library.dto;

import com.nutricoach.library.entity.WorkoutSection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutSectionResponse(
        UUID id,
        UUID assignmentId,
        String name,
        WorkoutSection.Type sectionType,
        String description,
        List<SectionExerciseResponse> exercises,
        Instant createdAt,
        Instant updatedAt) {}
