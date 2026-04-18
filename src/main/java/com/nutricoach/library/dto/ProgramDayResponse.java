package com.nutricoach.library.dto;

import java.util.UUID;

public record ProgramDayResponse(
        UUID id,
        int dayNumber,
        UUID workoutId,
        String workoutName,
        String notes) {}
