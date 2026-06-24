package com.nutricoach.library.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProgramAssignmentResponse(
        UUID id,
        UUID clientId,
        UUID programId,
        Instant assignedAt,
        LocalDate startDate,
        String notes) {}
