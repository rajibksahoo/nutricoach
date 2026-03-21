package com.nutricoach.progress.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LogProgressRequest(

        @NotNull(message = "Logged date is required")
        LocalDate loggedDate,

        @DecimalMin(value = "10.0", message = "Weight must be at least 10 kg")
        @DecimalMax(value = "500.0", message = "Weight must be under 500 kg")
        BigDecimal weightKg,

        @DecimalMin(value = "0.0", message = "Body fat must be 0% or more")
        @DecimalMax(value = "70.0", message = "Body fat must be 70% or less")
        BigDecimal bodyFatPercent,

        @DecimalMin(value = "30.0", message = "Waist must be at least 30 cm")
        @DecimalMax(value = "300.0", message = "Waist must be under 300 cm")
        BigDecimal waistCm,

        @DecimalMin(value = "30.0", message = "Chest must be at least 30 cm")
        @DecimalMax(value = "300.0", message = "Chest must be under 300 cm")
        BigDecimal chestCm,

        @DecimalMin(value = "30.0", message = "Hip must be at least 30 cm")
        @DecimalMax(value = "300.0", message = "Hip must be under 300 cm")
        BigDecimal hipCm,

        @Min(value = 0, message = "Adherence must be between 0 and 100")
        @Max(value = 100, message = "Adherence must be between 0 and 100")
        Integer adherencePercent,

        @Size(max = 1000, message = "Notes must be under 1000 characters")
        String notes
) {}
