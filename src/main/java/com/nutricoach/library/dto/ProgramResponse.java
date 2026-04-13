package com.nutricoach.library.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String description,
        int durationDays,
        List<ProgramDayResponse> days,
        Instant createdAt,
        Instant updatedAt) {}
