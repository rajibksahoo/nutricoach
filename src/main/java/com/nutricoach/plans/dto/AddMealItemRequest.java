package com.nutricoach.plans.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddMealItemRequest(

        @NotNull(message = "Food item ID is required")
        UUID foodItemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "1.0", message = "Quantity must be at least 1g")
        BigDecimal quantityGrams,

        String quantityUnit
) {}
