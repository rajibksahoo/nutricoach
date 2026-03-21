package com.nutricoach.progress.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        UUID clientId,
        UUID mealPlanId,
        LocalDate checkInDate,
        Integer adherencePercent,
        String clientNotes,
        String coachNotes
) {}
