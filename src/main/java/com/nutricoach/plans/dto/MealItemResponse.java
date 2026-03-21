package com.nutricoach.plans.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MealItemResponse(
        UUID id,
        UUID foodItemId,
        String foodItemName,
        BigDecimal quantityGrams,
        String quantityUnit,
        Integer calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG
) {}
