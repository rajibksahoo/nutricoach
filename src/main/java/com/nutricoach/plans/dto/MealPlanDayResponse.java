package com.nutricoach.plans.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MealPlanDayResponse(
        UUID id,
        int dayNumber,
        Integer totalCalories,
        BigDecimal totalProteinG,
        BigDecimal totalCarbsG,
        BigDecimal totalFatG,
        List<MealResponse> meals
) {}
