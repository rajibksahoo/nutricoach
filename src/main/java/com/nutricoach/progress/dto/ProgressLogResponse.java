package com.nutricoach.progress.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProgressLogResponse(
        UUID id,
        UUID clientId,
        LocalDate loggedDate,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal waistCm,
        BigDecimal chestCm,
        BigDecimal hipCm,
        Integer adherencePercent,
        String notes
) {}
