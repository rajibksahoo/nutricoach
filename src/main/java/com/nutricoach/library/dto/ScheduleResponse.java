package com.nutricoach.library.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID clientId,
        UUID workoutId,
        LocalDate date,
        String notes) {}
