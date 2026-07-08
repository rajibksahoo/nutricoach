package com.nutricoach.library.service;

import com.nutricoach.library.dto.ClientScheduledWorkoutResponse;
import com.nutricoach.library.dto.ClientWorkoutLineResponse;
import com.nutricoach.library.dto.SectionExerciseResponse;
import com.nutricoach.library.dto.WorkoutResponse;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.entity.ClientProgramAssignment;
import com.nutricoach.library.entity.ClientWorkoutCompletion;
import com.nutricoach.library.entity.Program;
import com.nutricoach.library.entity.ProgramDay;
import com.nutricoach.library.repository.ClientProgramAssignmentRepository;
import com.nutricoach.library.repository.ClientWorkoutCompletionRepository;
import com.nutricoach.library.repository.ProgramDayRepository;
import com.nutricoach.library.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Derives a client's upcoming workouts (date + exercise preview) live from
 * their active program assignments — no materialized schedule rows.
 */
@Service
@RequiredArgsConstructor
public class PortalWorkoutService {

    private final ClientProgramAssignmentRepository assignmentRepository;
    private final ProgramRepository programRepository;
    private final ProgramDayRepository programDayRepository;
    private final ClientWorkoutCompletionRepository completionRepository;
    private final WorkoutService workoutService;

    @Transactional(readOnly = true)
    public List<ClientScheduledWorkoutResponse> listUpcoming(UUID clientId, UUID coachId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<ClientScheduledWorkoutResponse> out = new ArrayList<>();
        // Cache derived exercise lines per distinct workout to avoid re-fetching.
        Map<UUID, WorkoutResponse> workoutCache = new HashMap<>();

        // Load all of this client's completions once (no N+1): key by workout+date.
        Set<String> completedKeys = new HashSet<>();
        for (ClientWorkoutCompletion c : completionRepository
                .findByCoachIdAndClientIdAndDeletedAtIsNull(coachId, clientId)) {
            completedKeys.add(completionKey(c.getWorkoutId(), c.getWorkoutDate()));
        }

        List<ClientProgramAssignment> assignments = assignmentRepository
                .findByCoachIdAndClientIdAndDeletedAtIsNullOrderByAssignedAtDesc(coachId, clientId);

        for (ClientProgramAssignment a : assignments) {
            Program program = programRepository
                    .findByIdAndCoachIdAndDeletedAtIsNull(a.getProgramId(), coachId)
                    .orElse(null);
            if (program == null) continue;

            LocalDate startDate = a.getStartDate() != null
                    ? a.getStartDate()
                    : a.getAssignedAt().atZone(ZoneId.systemDefault()).toLocalDate();

            for (ProgramDay day : programDayRepository.findByProgramIdOrderByDayNumberAsc(program.getId())) {
                if (day.getWorkoutId() == null) continue;
                LocalDate date = startDate.plusDays(day.getDayNumber() - 1L);
                if (date.isBefore(today)) continue;

                WorkoutResponse workout = workoutCache.computeIfAbsent(
                        day.getWorkoutId(), id -> loadWorkout(id, coachId));
                if (workout == null) continue; // workout deleted but still referenced

                List<ClientWorkoutLineResponse> lines = new ArrayList<>();
                if (workout.sections() != null) {
                    workout.sections().forEach(s -> {
                        if (s.exercises() != null) {
                            s.exercises().forEach(ex -> lines.add(
                                    new ClientWorkoutLineResponse(ex.exerciseName(), ex.sets(), buildTarget(ex))));
                        }
                    });
                }

                boolean completed = completedKeys.contains(completionKey(workout.id(), date));
                out.add(new ClientScheduledWorkoutResponse(
                        date, program.getId(), program.getName(),
                        workout.id(), workout.name(), lines.size(), lines, completed));
            }
        }

        out.sort(Comparator.comparing(ClientScheduledWorkoutResponse::date));
        return out;
    }

    /**
     * Marks one of the client's derived upcoming/today workouts as completed.
     * Idempotent: completing an already-completed (client, workout, date) is a
     * no-op that returns success. Rejects a (workoutId, date) that does not match
     * a currently-scheduled workout so clients cannot write junk rows.
     */
    @Transactional
    public ClientScheduledWorkoutResponse complete(UUID clientId, UUID coachId, UUID workoutId, LocalDate date) {
        ClientScheduledWorkoutResponse match = listUpcoming(clientId, coachId).stream()
                .filter(w -> w.workoutId().equals(workoutId) && w.date().equals(date))
                .findFirst()
                .orElseThrow(() -> NutriCoachException.notFound(
                        "No scheduled workout found for that workout and date"));

        // Idempotent via find-first: only insert when no active completion exists.
        completionRepository
                .findByCoachIdAndClientIdAndWorkoutIdAndWorkoutDateAndDeletedAtIsNull(
                        coachId, clientId, workoutId, date)
                .orElseGet(() -> completionRepository.save(ClientWorkoutCompletion.builder()
                        .coachId(coachId).clientId(clientId).workoutId(workoutId)
                        .workoutDate(date).completedAt(Instant.now()).build()));

        return new ClientScheduledWorkoutResponse(
                match.date(), match.programId(), match.programName(),
                match.workoutId(), match.workoutName(), match.exerciseCount(),
                match.exercises(), true);
    }

    private static String completionKey(UUID workoutId, LocalDate date) {
        return workoutId + "|" + date;
    }

    private WorkoutResponse loadWorkout(UUID workoutId, UUID coachId) {
        try {
            return workoutService.get(workoutId, coachId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String buildTarget(SectionExerciseResponse ex) {
        if (ex.weight() != null && !ex.weight().isBlank() && ex.reps() != null) {
            return ex.weight() + " x " + ex.reps();
        }
        if (ex.reps() != null) return ex.reps() + " reps";
        if (ex.durationSeconds() != null) return ex.durationSeconds() + "s";
        return "—";
    }
}
