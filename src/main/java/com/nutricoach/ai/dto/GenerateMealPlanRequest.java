package com.nutricoach.ai.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateMealPlanRequest(

        @NotNull(message = "Client ID is required")
        UUID clientId
) {}
