package com.nutricoach.library.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientScheduledWorkoutResponse(
        LocalDate date,
        UUID programId,
        String programName,
        UUID workoutId,
        String workoutName,
        int exerciseCount,
        List<ClientWorkoutLineResponse> exercises) {}
