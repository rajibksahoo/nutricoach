package com.nutricoach.plans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMealPlanRequest(

        @NotBlank(message = "Plan name is required")
        @Size(max = 150, message = "Name must be 150 characters or fewer")
        String name,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        Integer totalCaloriesTarget
) {}
