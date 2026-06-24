package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssignProgramRequest(
        @NotEmpty List<UUID> clientIds,
        LocalDate startDate,
        String notes) {}
