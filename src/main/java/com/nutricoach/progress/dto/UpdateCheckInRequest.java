package com.nutricoach.progress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Partial edit of a check-in. Null fields are left as they are, so a coach can
 * reply with {@code coachNotes} alone without touching what the client wrote.
 */
public record UpdateCheckInRequest(
        @Min(0) @Max(100) Integer adherencePercent,
        String clientNotes,
        String coachNotes) {}
