package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignWorkoutRequest(
        @NotEmpty List<UUID> clientIds,
        String notes) {}
