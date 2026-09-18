package com.nutricoach.client.dto;

import java.time.LocalDate;

/**
 * The Training card on client detail.
 *
 * <p>"Planned" counts what the client was expected to train — program-derived
 * days plus ad-hoc schedules — and "done" counts rows in
 * {@code client_workout_completions}. A client who never taps done therefore
 * reads as 0 planned-vs-done rather than as having no plan, which is why the
 * card distinguishes an empty plan from an untracked one.
 */
public record ClientTrainingStatsResponse(
        Window last7Days,
        Window last30Days,
        Window nextWeek,
        LastWorkout lastWorkout) {

    public record Window(int done, int planned) {}

    /** Null when the client has never completed a workout. */
    public record LastWorkout(String workoutName, LocalDate date, int daysAgo) {}
}
