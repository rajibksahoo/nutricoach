package com.nutricoach.library.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.ClientAssignmentsResponse;
import com.nutricoach.library.dto.ClientAssignmentsResponse.ProgramAssignmentItem;
import com.nutricoach.library.dto.ClientAssignmentsResponse.WorkoutAssignmentItem;
import com.nutricoach.library.entity.ClientProgramAssignment;
import com.nutricoach.library.entity.ClientWorkoutAssignment;
import com.nutricoach.library.entity.Program;
import com.nutricoach.library.entity.Workout;
import com.nutricoach.library.repository.ClientProgramAssignmentRepository;
import com.nutricoach.library.repository.ClientWorkoutAssignmentRepository;
import com.nutricoach.library.repository.ProgramRepository;
import com.nutricoach.library.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read model for the coach-side Training tab: what programs and workouts are
 * assigned to a given client.
 *
 * <p>Assignments were previously queryable only by program or by workout, which
 * is the wrong axis for a client-detail screen — a coach who assigned an
 * 8-week program saw nothing on the client's Training tab.
 */
@Service
@RequiredArgsConstructor
public class ClientTrainingService {

    private final ClientProgramAssignmentRepository programAssignmentRepository;
    private final ClientWorkoutAssignmentRepository workoutAssignmentRepository;
    private final ProgramRepository programRepository;
    private final WorkoutRepository workoutRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public ClientAssignmentsResponse listForClient(UUID clientId, UUID coachId) {
        requireClient(clientId, coachId);

        List<ClientProgramAssignment> programAssignments = programAssignmentRepository
                .findByCoachIdAndClientIdAndDeletedAtIsNullOrderByAssignedAtDesc(coachId, clientId);
        List<ClientWorkoutAssignment> workoutAssignments = workoutAssignmentRepository
                .findByCoachIdAndClientIdAndDeletedAtIsNullOrderByAssignedAtDesc(coachId, clientId);

        Map<UUID, Program> programs = byId(
                programRepository.findAllById(idsOf(programAssignments, ClientProgramAssignment::getProgramId)),
                Program::getId, p -> p.getCoachId().equals(coachId) && p.getDeletedAt() == null);
        Map<UUID, Workout> workouts = byId(
                workoutRepository.findAllById(idsOf(workoutAssignments, ClientWorkoutAssignment::getWorkoutId)),
                Workout::getId, w -> w.getCoachId().equals(coachId) && w.getDeletedAt() == null);

        // An assignment whose program/workout was deleted is dropped rather than
        // rendered as a nameless row — the coach can no longer act on it.
        List<ProgramAssignmentItem> programItems = programAssignments.stream()
                .filter(a -> programs.containsKey(a.getProgramId()))
                .map(a -> {
                    Program p = programs.get(a.getProgramId());
                    return new ProgramAssignmentItem(a.getId(), p.getId(), p.getName(),
                            a.getStartDate(), p.getWeeks(), a.getAssignedAt(), a.getNotes());
                })
                .toList();

        List<WorkoutAssignmentItem> workoutItems = workoutAssignments.stream()
                .filter(a -> workouts.containsKey(a.getWorkoutId()))
                .map(a -> {
                    Workout w = workouts.get(a.getWorkoutId());
                    return new WorkoutAssignmentItem(a.getId(), w.getId(), w.getName(),
                            a.getAssignedAt(), a.getNotes());
                })
                .toList();

        return new ClientAssignmentsResponse(programItems, workoutItems);
    }

    private static <T> List<UUID> idsOf(List<T> rows, Function<T, UUID> id) {
        return rows.stream().map(id).distinct().toList();
    }

    private static <T> Map<UUID, T> byId(List<T> rows, Function<T, UUID> id,
                                         java.util.function.Predicate<T> ownedByCoach) {
        return rows.stream().filter(ownedByCoach)
                .collect(Collectors.toMap(id, Function.identity()));
    }

    private void requireClient(UUID clientId, UUID coachId) {
        clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));
    }
}
