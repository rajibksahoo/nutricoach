package com.nutricoach.billing.dto;

import com.nutricoach.billing.entity.Subscription;
import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(
        @NotNull(message = "Plan tier is required")
        Subscription.PlanTier planTier
) {}
