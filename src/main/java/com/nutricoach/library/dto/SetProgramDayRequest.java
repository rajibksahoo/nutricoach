package com.nutricoach.library.dto;

import java.util.UUID;

public record SetProgramDayRequest(
        UUID workoutId,
        String notes) {}
