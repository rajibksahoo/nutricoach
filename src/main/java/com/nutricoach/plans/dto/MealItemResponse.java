package com.nutricoach.plans.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MealItemResponse(
        UUID id,
        UUID foodItemId,
        String foodItemName,
        /** True when the item names its own food rather than a curated one. */
        boolean custom,
        BigDecimal quantityGrams,
        String quantityUnit,
        Integer calories,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG
) {}
