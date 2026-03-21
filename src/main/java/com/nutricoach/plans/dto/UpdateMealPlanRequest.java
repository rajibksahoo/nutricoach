package com.nutricoach.plans.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMealPlanRequest(

        @Size(max = 150, message = "Name must be 150 characters or fewer")
        String name,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        Integer totalCaloriesTarget
) {}
