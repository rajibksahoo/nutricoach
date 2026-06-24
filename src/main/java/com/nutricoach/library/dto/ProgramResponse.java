package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String description,
        int durationDays,
        Integer weeks,
        String modality,
        String experienceLevel,
        List<String> tags,
        String coverImageUrl,
        String coverGradient,
        List<ProgramDayResponse> days,
        Instant createdAt,
        Instant updatedAt) {}
