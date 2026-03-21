package com.nutricoach.plans.dto;

import com.nutricoach.plans.entity.Meal;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UpdateMealRequest(

        Meal.MealType mealType,

        @Size(max = 150)
        String name,

        LocalTime timeOfDay,

        Integer sequenceOrder
) {}
