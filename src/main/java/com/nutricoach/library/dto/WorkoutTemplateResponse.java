package com.nutricoach.library.dto;

import com.nutricoach.library.entity.WorkoutTemplate;

import java.util.List;
import java.util.UUID;

public record WorkoutTemplateResponse(
        UUID id,
        String name,
        String description,
        String coverGradient,
        List<String> equipment,
        List<WorkoutTemplate.Section> sections) {}
