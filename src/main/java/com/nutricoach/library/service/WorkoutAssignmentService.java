package com.nutricoach.library.service;

import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.entity.*;
import com.nutricoach.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutAssignmentService {

    private final ClientWorkoutAssignmentRepository assignmentRepository;
    private final ClientWorkoutScheduleRepository scheduleRepository;
    private final WorkoutRepository workoutRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public List<AssignmentResponse> assign(UUID workoutId, UUID coachId, AssignWorkoutRequest req) {
        requireWorkout(workoutId, coachId);
        List<AssignmentResponse> result = new ArrayList<>();
        for (UUID clientId : req.clientIds()) {
            requireClient(clientId, coachId);
            ClientWorkoutAssignment existing = assignmentRepository
                    .findByClientIdAndWorkoutIdAndDeletedAtIsNull(clientId, workoutId).orElse(null);
            if (existing != null) {
                result.add(toAssignmentResponse(existing));
                continue;
            }
            ClientWorkoutAssignment saved = assignmentRepository.save(ClientWorkoutAssignment.builder()
                    .coachId(coachId)
                    .clientId(clientId)
                    .workoutId(workoutId)
                    .assignedAt(Instant.now())
                    .notes(req.notes())
                    .build());
            result.add(toAssignmentResponse(saved));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listByWorkout(UUID workoutId, UUID coachId) {
        requireWorkout(workoutId, coachId);
        return assignmentRepository.findByWorkoutIdAndDeletedAtIsNull(workoutId).stream()
                .filter(a -> a.getCoachId().equals(coachId))
                .map(this::toAssignmentResponse).toList();
    }

    @Transactional
    public void unassign(UUID workoutId, UUID assignmentId, UUID coachId) {
        requireWorkout(workoutId, coachId);
        ClientWorkoutAssignment a = assignmentRepository.findByIdAndCoachIdAndDeletedAtIsNull(assignmentId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Assignment not found"));
        if (!a.getWorkoutId().equals(workoutId)) {
            throw NutriCoachException.notFound("Assignment not found");
        }
        a.setDeletedAt(Instant.now());
        assignmentRepository.save(a);
    }

    @Transactional
    public ScheduleResponse schedule(UUID workoutId, UUID coachId, ScheduleWorkoutRequest req) {
        requireWorkout(workoutId, coachId);
        requireClient(req.clientId(), coachId);
        ClientWorkoutSchedule saved = scheduleRepository.save(ClientWorkoutSchedule.builder()
                .coachId(coachId)
                .clientId(req.clientId())
                .workoutId(workoutId)
                .scheduledDate(req.date())
                .notes(req.notes())
                .build());
        return toScheduleResponse(saved);
    }

    @Transactional
    public void unschedule(UUID scheduleId, UUID coachId) {
        ClientWorkoutSchedule s = scheduleRepository.findByIdAndCoachIdAndDeletedAtIsNull(scheduleId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Schedule not found"));
        s.setDeletedAt(Instant.now());
        scheduleRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedules(UUID clientId, UUID coachId, LocalDate from, LocalDate to) {
        requireClient(clientId, coachId);
        List<ClientWorkoutSchedule> schedules = (from != null && to != null)
                ? scheduleRepository.findByCoachIdAndClientIdAndScheduledDateBetweenAndDeletedAtIsNullOrderByScheduledDateAsc(coachId, clientId, from, to)
                : scheduleRepository.findByCoachIdAndClientIdAndDeletedAtIsNullOrderByScheduledDateAsc(coachId, clientId);
        return schedules.stream().map(this::toScheduleResponse).toList();
    }

    private void requireWorkout(UUID workoutId, UUID coachId) {
        workoutRepository.findByIdAndCoachIdAndDeletedAtIsNull(workoutId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Workout not found"));
    }

    private void requireClient(UUID clientId, UUID coachId) {
        clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));
    }

    private AssignmentResponse toAssignmentResponse(ClientWorkoutAssignment a) {
        return new AssignmentResponse(a.getId(), a.getClientId(), a.getWorkoutId(), a.getAssignedAt(), a.getNotes());
    }

    private ScheduleResponse toScheduleResponse(ClientWorkoutSchedule s) {
        return new ScheduleResponse(s.getId(), s.getClientId(), s.getWorkoutId(), s.getScheduledDate(), s.getNotes());
    }
}
