package com.nutricoach.coach.service;

import com.nutricoach.billing.service.SubscriptionGate;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.dto.DashboardOverviewResponse;
import com.nutricoach.coach.dto.DashboardOverviewResponse.ActionItem;
import com.nutricoach.coach.dto.DashboardOverviewResponse.ActivityItem;
import com.nutricoach.coach.dto.DashboardOverviewResponse.CheckInToday;
import com.nutricoach.coach.dto.DashboardOverviewResponse.Counts;
import com.nutricoach.coach.dto.DashboardOverviewResponse.Roster;
import com.nutricoach.coach.dto.DashboardOverviewResponse.RosterClient;
import com.nutricoach.coach.dto.DashboardOverviewResponse.ScheduledSession;
import com.nutricoach.coach.dto.DashboardOverviewResponse.SubscriptionInfo;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.entity.ClientProgramAssignment;
import com.nutricoach.library.entity.ClientWorkoutSchedule;
import com.nutricoach.library.entity.ProgramDay;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.repository.ClientProgramAssignmentRepository;
import com.nutricoach.library.repository.ClientWorkoutScheduleRepository;
import com.nutricoach.library.repository.ProgramDayRepository;
import com.nutricoach.library.repository.WorkoutRepository;
import com.nutricoach.messaging.entity.Message;
import com.nutricoach.messaging.repository.MessageRepository;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.entity.ProgressLog;
import com.nutricoach.progress.repository.CheckInRepository;
import com.nutricoach.progress.repository.ProgressLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the coach dashboard in a fixed number of coach-scoped queries.
 *
 * Every signal it needs is exposed elsewhere only per-client, so assembling this
 * in the browser would cost roughly three requests per client. Aggregating here
 * keeps the cost flat as a roster grows.
 */
@Service
@RequiredArgsConstructor
public class DashboardOverviewService {

    /**
     * A client is "overdue" after this many days without a check-in. Mirrors
     * CheckInReminderScheduler so the dashboard and the WhatsApp nudge agree.
     */
    private static final int CHECK_IN_OVERDUE_DAYS = 7;

    /** How far ahead a meal plan's end date counts as "expiring soon". */
    private static final int PLAN_EXPIRY_WINDOW_DAYS = 7;

    private static final int ACTION_QUEUE_LIMIT = 25;
    private static final int ACTIVITY_LIMIT = 20;

    private static final int PRIORITY_UNANSWERED_MESSAGE = 1;
    private static final int PRIORITY_OVERDUE_CHECKIN    = 2;
    private static final int PRIORITY_PLAN_EXPIRING      = 3;
    private static final int PRIORITY_NO_MEAL_PLAN       = 4;
    private static final int PRIORITY_NEW_CLIENT         = 5;

    private final CoachRepository coachRepository;
    private final ClientRepository clientRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MessageRepository messageRepository;
    private final CheckInRepository checkInRepository;
    private final ProgressLogRepository progressLogRepository;
    private final ClientWorkoutScheduleRepository scheduleRepository;
    private final ClientProgramAssignmentRepository assignmentRepository;
    private final ProgramDayRepository programDayRepository;
    private final WorkoutRepository workoutRepository;
    private final SubscriptionGate subscriptionGate;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(UUID coachId) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Coach not found"));

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        // Load the roster once — every name below is resolved from this map, never re-queried.
        List<Client> clients = clientRepository.findByCoachIdAndDeletedAtIsNull(coachId);
        Map<UUID, Client> clientsById = clients.stream()
                .collect(Collectors.toMap(Client::getId, Function.identity(), (a, b) -> a));

        List<ActionItem> queue = new ArrayList<>();

        // ── Unanswered messages ──────────────────────────────────────────────
        List<MessageRepository.UnreadConversation> unread = messageRepository.findUnreadConversations(coachId);
        for (MessageRepository.UnreadConversation u : unread) {
            Client c = clientsById.get(u.getClientId());
            if (c == null) continue; // client deleted; conversation is orphaned
            queue.add(action(PRIORITY_UNANSWERED_MESSAGE, "UNANSWERED_MESSAGE", c,
                    "Waiting on your reply",
                    u.getUnreadCount() == 1 ? "1 unread message" : u.getUnreadCount() + " unread messages",
                    u.getLastMessageAt()));
        }

        // ── Overdue check-ins ────────────────────────────────────────────────
        Map<UUID, LocalDate> lastCheckIn = checkInRepository.findLastCheckInPerClient(coachId).stream()
                .collect(Collectors.toMap(CheckInRepository.LastCheckIn::getClientId,
                        CheckInRepository.LastCheckIn::getLastDate, (a, b) -> a.isAfter(b) ? a : b));

        long overdueCheckIns = 0;
        for (Client c : clients) {
            if (c.getStatus() != Client.Status.ACTIVE) continue;

            LocalDate last = lastCheckIn.get(c.getId());
            // A client who joined three days ago and never checked in is not overdue yet —
            // fall back to their join date so new clients don't flood the queue.
            LocalDate since = last != null ? last : c.getCreatedAt().atZone(zone).toLocalDate();
            long days = ChronoUnit.DAYS.between(since, today);
            if (days < CHECK_IN_OVERDUE_DAYS) continue;

            overdueCheckIns++;
            queue.add(action(PRIORITY_OVERDUE_CHECKIN, "OVERDUE_CHECKIN", c,
                    "No check-in for " + days + " days",
                    last != null ? "Last check-in " + last : "Never checked in",
                    since.atStartOfDay(zone).toInstant()));
        }

        // ── Meal plans expiring soon ─────────────────────────────────────────
        List<MealPlan> expiring = mealPlanRepository.findByCoachIdAndStatusAndDeletedAtIsNullAndEndDateBetween(
                coachId, MealPlan.Status.ACTIVE, today, today.plusDays(PLAN_EXPIRY_WINDOW_DAYS));
        for (MealPlan p : expiring) {
            Client c = clientsById.get(p.getClientId());
            if (c == null) continue;
            long days = ChronoUnit.DAYS.between(today, p.getEndDate());
            queue.add(action(PRIORITY_PLAN_EXPIRING, "PLAN_EXPIRING", c,
                    days == 0 ? "Meal plan ends today" : "Meal plan ends in " + days + " days",
                    p.getName(),
                    p.getEndDate().atStartOfDay(zone).toInstant()));
        }

        // ── Clients with no meal plan at all ─────────────────────────────────
        List<Client> needingPlan = clientRepository.findClientsWithoutMealPlan(coachId);
        for (Client c : needingPlan) {
            queue.add(action(PRIORITY_NO_MEAL_PLAN, "NO_MEAL_PLAN", c,
                    "No meal plan yet", "Client since " + c.getCreatedAt().atZone(zone).toLocalDate(),
                    c.getCreatedAt()));
        }

        // ── Clients still onboarding ─────────────────────────────────────────
        for (Client c : clients) {
            if (c.getStatus() != Client.Status.ONBOARDING) continue;
            queue.add(action(PRIORITY_NEW_CLIENT, "NEW_CLIENT", c,
                    "Still onboarding", "Joined " + c.getCreatedAt().atZone(zone).toLocalDate(),
                    c.getCreatedAt()));
        }

        // Lowest priority first, then longest-waiting first inside a priority.
        queue.sort(Comparator.comparingInt(ActionItem::priority)
                .thenComparing(ActionItem::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())));
        List<ActionItem> actionQueue = queue.size() > ACTION_QUEUE_LIMIT
                ? List.copyOf(queue.subList(0, ACTION_QUEUE_LIMIT))
                : List.copyOf(queue);

        // ── Today ────────────────────────────────────────────────────────────
        DashboardOverviewResponse.Today todayPanel =
                buildToday(coachId, today, zone, clientsById);

        // ── Roster ───────────────────────────────────────────────────────────
        int limit = subscriptionGate.clientLimitFor(coach);
        List<RosterClient> recent = clientRepository
                .findTop5ByCoachIdAndDeletedAtIsNullOrderByCreatedAtDesc(coachId).stream()
                .map(c -> new RosterClient(c.getId(), c.getName(), c.getPhone(),
                        c.getStatus().name(), c.getCreatedAt()))
                .toList();
        Roster roster = new Roster(limit == Integer.MAX_VALUE ? -1 : limit, recent);

        // ── Subscription ─────────────────────────────────────────────────────
        Integer daysLeft = null;
        if (coach.getSubscriptionStatus() == Coach.SubscriptionStatus.TRIAL && coach.getTrialEndsAt() != null) {
            long d = ChronoUnit.DAYS.between(Instant.now(), coach.getTrialEndsAt());
            daysLeft = (int) Math.max(0, d);
        }
        SubscriptionInfo subscription = new SubscriptionInfo(
                coach.getSubscriptionTier().name(),
                coach.getSubscriptionStatus().name(),
                coach.getTrialEndsAt(),
                daysLeft);

        // ── Counts ───────────────────────────────────────────────────────────
        Counts counts = new Counts(
                clients.size(),
                countByStatus(clients, Client.Status.ACTIVE),
                countByStatus(clients, Client.Status.ONBOARDING),
                countByStatus(clients, Client.Status.INACTIVE),
                unread.size(),
                overdueCheckIns,
                needingPlan.size(),
                expiring.size());

        return new DashboardOverviewResponse(
                counts, actionQueue, todayPanel, roster, subscription,
                buildActivity(coachId, clientsById, zone));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private DashboardOverviewResponse.Today buildToday(
            UUID coachId, LocalDate today, ZoneId zone, Map<UUID, Client> clientsById) {

        List<ScheduledSession> sessions = new ArrayList<>();
        Set<UUID> workoutIds = new HashSet<>();

        // Explicitly booked sessions.
        List<ClientWorkoutSchedule> booked =
                scheduleRepository.findByCoachIdAndScheduledDateAndDeletedAtIsNull(coachId, today);
        booked.forEach(s -> workoutIds.add(s.getWorkoutId()));

        // Sessions derived live from active program assignments — same day-number
        // arithmetic PortalWorkoutService uses to build the client's own list.
        List<ClientProgramAssignment> assignments = assignmentRepository.findByCoachIdAndDeletedAtIsNull(coachId);
        Map<UUID, Map<Integer, UUID>> daysByProgram = new HashMap<>();
        Map<UUID, UUID> derived = new LinkedHashMap<>(); // clientId → workoutId for today

        for (ClientProgramAssignment a : assignments) {
            LocalDate start = programStartDate(a, zone);
            int dayNumber = (int) ChronoUnit.DAYS.between(start, today) + 1;
            if (dayNumber < 1) continue; // hasn't started

            Map<Integer, UUID> days = daysByProgram.computeIfAbsent(a.getProgramId(), pid ->
                    programDayRepository.findByProgramIdOrderByDayNumberAsc(pid).stream()
                            .filter(d -> d.getWorkoutId() != null)
                            .collect(Collectors.toMap(ProgramDay::getDayNumber, ProgramDay::getWorkoutId,
                                    (x, y) -> x)));

            UUID workoutId = days.get(dayNumber);
            if (workoutId == null) continue; // rest day, or program already finished
            derived.putIfAbsent(a.getClientId(), workoutId);
            workoutIds.add(workoutId);
        }

        // Resolve every workout name in one query, tenant-filtered.
        Map<UUID, String> workoutNames = workoutIds.isEmpty() ? Map.of()
                : workoutRepository.findAllById(workoutIds).stream()
                        .filter(w -> coachId.equals(w.getCoachId()) && w.getDeletedAt() == null)
                        .collect(Collectors.toMap(Workout::getId, Workout::getName, (a, b) -> a));

        for (ClientWorkoutSchedule s : booked) {
            Client c = clientsById.get(s.getClientId());
            String name = workoutNames.get(s.getWorkoutId());
            if (c == null || name == null) continue;
            sessions.add(new ScheduledSession(c.getId(), c.getName(), s.getWorkoutId(), name, "SCHEDULE"));
        }
        derived.forEach((clientId, workoutId) -> {
            Client c = clientsById.get(clientId);
            String name = workoutNames.get(workoutId);
            if (c == null || name == null) return;
            sessions.add(new ScheduledSession(c.getId(), c.getName(), workoutId, name, "PROGRAM"));
        });
        sessions.sort(Comparator.comparing(ScheduledSession::clientName, String.CASE_INSENSITIVE_ORDER));

        List<CheckInToday> checkIns = checkInRepository.findByCoachIdAndCheckInDate(coachId, today).stream()
                .filter(ci -> clientsById.containsKey(ci.getClientId()))
                .map(ci -> new CheckInToday(ci.getClientId(),
                        clientsById.get(ci.getClientId()).getName(), ci.getAdherencePercent()))
                .sorted(Comparator.comparing(CheckInToday::clientName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new DashboardOverviewResponse.Today(List.copyOf(sessions), checkIns);
    }

    /** An assignment without an explicit start date begins the day it was assigned. */
    private static LocalDate programStartDate(ClientProgramAssignment a, ZoneId zone) {
        return a.getStartDate() != null
                ? a.getStartDate()
                : a.getAssignedAt().atZone(zone).toLocalDate();
    }

    private List<ActivityItem> buildActivity(UUID coachId, Map<UUID, Client> clientsById, ZoneId zone) {
        List<ActivityItem> items = new ArrayList<>();

        for (Message m : messageRepository.findTop15ByCoachIdOrderByCreatedAtDesc(coachId)) {
            Client c = clientsById.get(m.getClientId());
            if (c == null) continue;
            String who = m.getSenderType() == Message.SenderType.CLIENT ? "Sent" : "You replied";
            items.add(new ActivityItem("MESSAGE", c.getId(), c.getName(),
                    who + ": " + truncate(m.getContent()), m.getCreatedAt()));
        }

        for (CheckIn ci : checkInRepository.findTop15ByCoachIdOrderByCreatedAtDesc(coachId)) {
            Client c = clientsById.get(ci.getClientId());
            if (c == null) continue;
            String summary = ci.getAdherencePercent() != null
                    ? "Checked in — " + ci.getAdherencePercent() + "% adherence"
                    : "Checked in";
            items.add(new ActivityItem("CHECK_IN", c.getId(), c.getName(), summary, ci.getCreatedAt()));
        }

        for (ProgressLog p : progressLogRepository.findTop15ByCoachIdOrderByCreatedAtDesc(coachId)) {
            Client c = clientsById.get(p.getClientId());
            if (c == null) continue;
            String summary = p.getWeightKg() != null
                    ? "Logged weight " + p.getWeightKg() + " kg"
                    : "Logged progress";
            items.add(new ActivityItem("PROGRESS_LOG", c.getId(), c.getName(), summary, p.getCreatedAt()));
        }

        for (Client c : clientsById.values()) {
            items.add(new ActivityItem("CLIENT_JOINED", c.getId(), c.getName(),
                    "Joined as " + c.getStatus().name().toLowerCase(), c.getCreatedAt()));
        }

        items.sort(Comparator.comparing(ActivityItem::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items.size() > ACTIVITY_LIMIT
                ? List.copyOf(items.subList(0, ACTIVITY_LIMIT))
                : List.copyOf(items);
    }

    private static ActionItem action(int priority, String type, Client c,
                                     String title, String detail, Instant occurredAt) {
        return new ActionItem(type, priority, c.getId(), c.getName(), c.getPhone(),
                c.getStatus().name(), title, detail, occurredAt);
    }

    private static long countByStatus(List<Client> clients, Client.Status status) {
        return clients.stream().filter(c -> c.getStatus() == status).count();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 80 ? s : s.substring(0, 79) + "…";
    }
}
