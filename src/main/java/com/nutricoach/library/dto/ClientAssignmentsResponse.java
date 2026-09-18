package com.nutricoach.library.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Everything a coach has assigned to one client, for the client-detail
 * Training tab. Dated one-off schedules are a separate concern and stay on
 * {@code GET /library/clients/{clientId}/schedules}.
 *
 * <p>Names are resolved server-side on purpose: the tab previously fetched the
 * coach's whole workout list to turn ids into labels, and cached it for the
 * session, so a rename showed stale text until reload.
 *
 * <p>The nested records carry distinct simple names ({@code ProgramAssignmentItem},
 * not {@code ProgramAssignment}) because springdoc keys OpenAPI components by
 * simple name — a collision silently drops one schema from the generated client.
 */
public record ClientAssignmentsResponse(
        List<ProgramAssignmentItem> programs,
        List<WorkoutAssignmentItem> workouts) {

    public record ProgramAssignmentItem(
            UUID id,
            UUID programId,
            String name,
            LocalDate startDate,
            Integer weeks,
            Instant assignedAt,
            String notes) {}

    public record WorkoutAssignmentItem(
            UUID id,
            UUID workoutId,
            String name,
            Instant assignedAt,
            String notes) {}
}
