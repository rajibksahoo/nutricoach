package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.UUID;

public record ProgramSummaryResponse(
        UUID id,
        String name,
        String description,
        int durationDays,
        Instant createdAt,
        Instant updatedAt) {}
