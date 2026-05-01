package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleWorkoutRequest(
        @NotNull UUID clientId,
        @NotNull LocalDate date,
        String notes) {}
