package com.nutricoach.progress.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record CreateCheckInRequest(

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        @NotNull(message = "Meal plan ID is required")
        UUID mealPlanId,

        @Min(value = 0, message = "Adherence must be between 0 and 100")
        @Max(value = 100, message = "Adherence must be between 0 and 100")
        Integer adherencePercent,

        @Size(max = 1000, message = "Notes must be under 1000 characters")
        String clientNotes,

        @Size(max = 1000, message = "Notes must be under 1000 characters")
        String coachNotes
) {}
