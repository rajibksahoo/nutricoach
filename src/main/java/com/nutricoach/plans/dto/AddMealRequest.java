package com.nutricoach.plans.dto;

import com.nutricoach.plans.entity.Meal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record AddMealRequest(

        @NotNull(message = "Meal type is required")
        Meal.MealType mealType,

        @NotBlank(message = "Meal name is required")
        @Size(max = 150)
        String name,

        LocalTime timeOfDay,

        int sequenceOrder
) {}
