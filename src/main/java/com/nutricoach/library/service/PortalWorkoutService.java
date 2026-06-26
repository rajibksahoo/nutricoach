package com.nutricoach.library.service;

import com.nutricoach.library.dto.ClientScheduledWorkoutResponse;
import com.nutricoach.library.dto.ClientWorkoutLineResponse;
import com.nutricoach.library.dto.SectionExerciseResponse;
import com.nutricoach.library.dto.WorkoutResponse;
import com.nutricoach.library.entity.ClientProgramAssignment;
import com.nutricoach.library.entity.Program;
import com.nutricoach.library.entity.ProgramDay;
import com.nutricoach.library.repository.ClientProgramAssignmentRepository;
import com.nutricoach.library.repository.ProgramDayRepository;
import com.nutricoach.library.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final WorkoutService workoutService;

    @Transactional(readOnly = true)
    public List<ClientScheduledWorkoutResponse> listUpcoming(UUID clientId, UUID coachId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<ClientScheduledWorkoutResponse> out = new ArrayList<>();
        // Cache derived exercise lines per distinct workout to avoid re-fetching.
        Map<UUID, WorkoutResponse> workoutCache = new HashMap<>();

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

                out.add(new ClientScheduledWorkoutResponse(
                        date, program.getId(), program.getName(),
                        workout.id(), workout.name(), lines.size(), lines));
            }
        }

        out.sort(Comparator.comparing(ClientScheduledWorkoutResponse::date));
        return out;
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
