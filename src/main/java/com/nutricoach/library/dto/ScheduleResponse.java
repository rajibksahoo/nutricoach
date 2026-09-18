package com.nutricoach.library.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A workout scheduled for a client on a date.
 *
 * <p>{@code workoutName} is resolved server-side: the coach-side Training tab
 * used to fetch the coach's entire workout list to turn ids into labels and
 * cache it for the session, so a renamed workout showed stale text.
 */
public record ScheduleResponse(
        UUID id,
        UUID clientId,
        UUID workoutId,
        String workoutName,
        LocalDate date,
        String notes) {}
