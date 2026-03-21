package com.nutricoach.plans.dto;

import com.nutricoach.plans.entity.FoodItem;

import java.math.BigDecimal;
import java.util.UUID;

public record FoodItemResponse(
        UUID id,
        String name,
        String nameHindi,
        String nameRegional,
        FoodItem.CuisineType cuisineType,
        FoodItem.Category category,
        BigDecimal caloriesPer100g,
        BigDecimal proteinPer100g,
        BigDecimal carbsPer100g,
        BigDecimal fatPer100g,
        BigDecimal fiberPer100g,
        FoodItem.Source source
) {}
