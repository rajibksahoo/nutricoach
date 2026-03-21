package com.nutricoach.plans.dto;

import com.nutricoach.plans.entity.MealPlan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MealPlanSummaryResponse(
        UUID id,
        UUID clientId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        MealPlan.Status status,
        boolean aiGenerated,
        Integer totalCaloriesTarget,
        Instant createdAt,
        Instant updatedAt
) {}
