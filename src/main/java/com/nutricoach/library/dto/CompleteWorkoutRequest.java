package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Client marks one of their derived upcoming/today workouts as done.
 * The (workoutId, date) pair must correspond to an actually-scheduled workout.
 */
public record CompleteWorkoutRequest(
        @NotNull UUID workoutId,
        @NotNull LocalDate date) {}
