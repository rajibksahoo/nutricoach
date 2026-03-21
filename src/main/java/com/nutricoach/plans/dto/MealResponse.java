package com.nutricoach.plans.dto;

import com.nutricoach.plans.entity.Meal;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MealResponse(
        UUID id,
        Meal.MealType mealType,
        String name,
        LocalTime timeOfDay,
        int sequenceOrder,
        List<MealItemResponse> items
) {}
