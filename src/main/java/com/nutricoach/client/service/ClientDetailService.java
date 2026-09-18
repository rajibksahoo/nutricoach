package com.nutricoach.client.service;

import com.nutricoach.client.dto.ClientActivityResponse;
import com.nutricoach.client.dto.ClientTrainingStatsResponse;
import com.nutricoach.client.dto.ClientTrainingStatsResponse.LastWorkout;
import com.nutricoach.client.dto.ClientTrainingStatsResponse.Window;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.util.Measures;
import com.nutricoach.library.entity.ClientWorkoutCompletion;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.repository.ClientWorkoutCompletionRepository;
import com.nutricoach.library.repository.WorkoutRepository;
import com.nutricoach.library.service.PortalWorkoutService;
import com.nutricoach.library.service.PortalWorkoutService.PlannedWorkout;
import com.nutricoach.messaging.entity.Message;
import com.nutricoach.messaging.repository.MessageRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read models behind the client-detail Training and Updates cards.
 *
 * <p>Both were visual slots with no data: Training showed four hardcoded zeros
 * and Updates was permanently empty. Neither was buildable when the screen was
 * designed — completions, schedules and program assignments all landed later.
 */
@Service
@RequiredArgsConstructor
public class ClientDetailService {

    /** Matches the Updates card, which shows a short recent history, not an archive. */
    private static final int ACTIVITY_LIMIT = 20;
    private static final int SUMMARY_MAX = 80;

    private final ClientRepository clientRepository;
    private final ClientWorkoutCompletionRepository completionRepository;
    private final WorkoutRepository workoutRepository;
    private final PortalWorkoutService portalWorkoutService;
    private final MessageRepository messageRepository;
    private final CheckInRepository checkInRepository;
    private final ProgressLogRepository progressLogRepository;

    @Transactional(readOnly = true)
    public ClientTrainingStatsResponse getTrainingStats(UUID clientId, UUID coachId) {
        requireClient(clientId, coachId);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        List<ClientWorkoutCompletion> completions = completionRepository
                .findByCoachIdAndClientIdAndDeletedAtIsNull(coachId, clientId);

        return new ClientTrainingStatsResponse(
                window(clientId, coachId, completions, today.minusDays(6), today),
                window(clientId, coachId, completions, today.minusDays(29), today),
                // Forward-looking, so nothing is done yet by definition.
                new Window(0, portalWorkoutService
                        .plannedBetween(clientId, coachId, today.plusDays(1), today.plusDays(7)).size()),
                lastWorkout(coachId, completions, today));
    }

    private Window window(UUID clientId, UUID coachId,
                          List<ClientWorkoutCompletion> completions,
                          LocalDate from, LocalDate to) {
        Set<PlannedWorkout> planned = portalWorkoutService.plannedBetween(clientId, coachId, from, to);
        long done = completions.stream()
                .filter(c -> !c.getWorkoutDate().isBefore(from) && !c.getWorkoutDate().isAfter(to))
                .count();
        return new Window((int) done, planned.size());
    }

    private LastWorkout lastWorkout(UUID coachId, List<ClientWorkoutCompletion> completions, LocalDate today) {
        return completions.stream()
                .max(Comparator.comparing(ClientWorkoutCompletion::getWorkoutDate))
                .map(c -> {
                    String name = workoutRepository.findByIdAndCoachIdAndDeletedAtIsNull(c.getWorkoutId(), coachId)
                            .map(Workout::getName)
                            .orElse("Workout");
                    int daysAgo = (int) ChronoUnit.DAYS.between(c.getWorkoutDate(), today);
                    return new LastWorkout(name, c.getWorkoutDate(), Math.max(0, daysAgo));
                })
                .orElse(null);
    }

    /**
     * The client's recent activity, newest first.
     *
     * <p>Derived by unioning the source tables rather than reading an
     * activity_log, so no existing write path has to remember to log. The cost
     * is a handful of small indexed reads per request, which is fine for a
     * single client-detail view.
     */
    @Transactional(readOnly = true)
    public List<ClientActivityResponse> getActivity(UUID clientId, UUID coachId) {
        Client client = requireClient(clientId, coachId);
        List<ClientActivityResponse> items = new ArrayList<>();

        for (Message m : messageRepository.findTop15ByCoachIdAndClientIdOrderByCreatedAtDesc(coachId, clientId)) {
            String who = m.getSenderType() == Message.SenderType.CLIENT ? "Sent" : "You replied";
            items.add(new ClientActivityResponse("MESSAGE", clientId,
                    who + ": " + truncate(m.getContent()), m.getCreatedAt()));
        }

        for (CheckIn ci : checkInRepository.findTop15ByCoachIdAndClientIdAndDeletedAtIsNullOrderByCreatedAtDesc(coachId, clientId)) {
            items.add(new ClientActivityResponse("CHECK_IN", clientId,
                    ci.getAdherencePercent() != null
                            ? "Checked in — " + ci.getAdherencePercent() + "% adherence"
                            : "Checked in",
                    ci.getCreatedAt()));
        }

        for (ProgressLog p : progressLogRepository.findTop15ByCoachIdAndClientIdOrderByCreatedAtDesc(coachId, clientId)) {
            items.add(new ClientActivityResponse("PROGRESS_LOG", clientId,
                    p.getWeightKg() != null ? "Logged weight " + Measures.formatKg(p.getWeightKg()) + " kg" : "Logged progress",
                    p.getCreatedAt()));
        }

        List<ClientWorkoutCompletion> completions = completionRepository
                .findByCoachIdAndClientIdAndDeletedAtIsNull(coachId, clientId);
        Map<UUID, String> workoutNames = workoutRepository
                .findAllById(completions.stream().map(ClientWorkoutCompletion::getWorkoutId).distinct().toList())
                .stream()
                .filter(w -> w.getCoachId().equals(coachId))
                .collect(Collectors.toMap(Workout::getId, Workout::getName));
        for (ClientWorkoutCompletion c : completions) {
            items.add(new ClientActivityResponse("WORKOUT_DONE", clientId,
                    "Completed " + workoutNames.getOrDefault(c.getWorkoutId(), "a workout"),
                    c.getCompletedAt()));
        }

        items.add(new ClientActivityResponse("CLIENT_JOINED", clientId,
                "Joined as " + client.getStatus().name().toLowerCase(), client.getCreatedAt()));

        items.sort(Comparator.comparing(ClientActivityResponse::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items.size() > ACTIVITY_LIMIT ? List.copyOf(items.subList(0, ACTIVITY_LIMIT)) : List.copyOf(items);
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= SUMMARY_MAX ? s : s.substring(0, SUMMARY_MAX - 1) + "…";
    }

    private Client requireClient(UUID clientId, UUID coachId) {
        return clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));
    }
}
