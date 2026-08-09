package com.nutricoach.coach.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything the coach dashboard renders, aggregated server-side in a fixed
 * number of coach-scoped queries (never proportional to roster size).
 */
public record DashboardOverviewResponse(
        Counts counts,
        List<ActionItem> actionQueue,
        Today today,
        Roster roster,
        SubscriptionInfo subscription,
        List<ActivityItem> activity
) {

    public record Counts(
            long totalClients,
            long activeClients,
            long onboardingClients,
            long inactiveClients,
            /** Conversations with at least one unread client message. */
            long unansweredMessages,
            /** Active clients with no check-in inside the reminder window. */
            long overdueCheckIns,
            long clientsNeedingPlan,
            /** ACTIVE meal plans whose endDate falls inside the next week. */
            long plansExpiringSoon
    ) {}

    /**
     * One row of the prioritised action queue.
     *
     * @param type one of UNANSWERED_MESSAGE, OVERDUE_CHECKIN, PLAN_EXPIRING,
     *             NO_MEAL_PLAN, NEW_CLIENT
     * @param priority lower sorts first
     * @param occurredAt when the item started waiting — the queue is oldest-first within a priority
     */
    public record ActionItem(
            String type,
            int priority,
            UUID clientId,
            String clientName,
            String clientPhone,
            String clientStatus,
            String title,
            String detail,
            Instant occurredAt
    ) {}

    public record Today(
            List<ScheduledSession> sessions,
            List<CheckInToday> checkIns
    ) {}

    /** @param source SCHEDULE for an explicit booking, PROGRAM when derived from an assignment */
    public record ScheduledSession(
            UUID clientId,
            String clientName,
            UUID workoutId,
            String workoutName,
            String source
    ) {}

    public record CheckInToday(
            UUID clientId,
            String clientName,
            Integer adherencePercent
    ) {}

    /** @param clientLimit tier cap on clients, or -1 when unlimited */
    public record Roster(
            long clientLimit,
            List<RosterClient> recentClients
    ) {}

    public record RosterClient(
            UUID id,
            String name,
            String phone,
            String status,
            Instant createdAt
    ) {}

    public record SubscriptionInfo(
            String tier,
            String status,
            Instant trialEndsAt,
            /** Null unless the coach is actually on trial; floors at 0. */
            Integer daysLeftInTrial
    ) {}

    /** @param type one of MESSAGE, CHECK_IN, PROGRESS_LOG, CLIENT_JOINED */
    public record ActivityItem(
            String type,
            UUID clientId,
            String clientName,
            String summary,
            Instant occurredAt
    ) {}
}
