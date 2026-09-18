package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A program as it appears in the Programs list.
 *
 * <p>{@code equipment} is derived, not stored: it is the distinct equipment of
 * every exercise inside the workouts placed on the program's days. Computing it
 * server-side keeps the list to one extra query instead of the browser walking
 * program → day → workout → section → exercise per row.
 */
public record ProgramSummaryResponse(
        UUID id,
        String name,
        String description,
        int durationDays,
        Integer weeks,
        String modality,
        String experienceLevel,
        List<String> tags,
        List<String> equipment,
        String coverImageUrl,
        String coverGradient,
        Instant createdAt,
        Instant updatedAt) {}
