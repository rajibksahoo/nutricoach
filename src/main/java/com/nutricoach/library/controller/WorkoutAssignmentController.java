package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.service.WorkoutAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Library — Assignments & Schedules", description = "Assign workouts to clients and schedule them for specific dates")
public class WorkoutAssignmentController {

    private final WorkoutAssignmentService assignmentService;
    private final SecurityUtils securityUtils;

    @PostMapping("/api/v1/library/workouts/{workoutId}/assignments")
    @Operation(summary = "Assign a workout to one or more clients")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> assign(
            @PathVariable UUID workoutId,
            @Valid @RequestBody AssignWorkoutRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workout assigned", assignmentService.assign(workoutId, coachId, req)));
    }

    @GetMapping("/api/v1/library/workouts/{workoutId}/assignments")
    @Operation(summary = "List active assignments for a workout")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(@PathVariable UUID workoutId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listByWorkout(workoutId, coachId)));
    }

    @DeleteMapping("/api/v1/library/workouts/{workoutId}/assignments/{assignmentId}")
    @Operation(summary = "Soft-delete an assignment")
    public ResponseEntity<ApiResponse<Void>> unassign(@PathVariable UUID workoutId,
                                                      @PathVariable UUID assignmentId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        assignmentService.unassign(workoutId, assignmentId, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Assignment removed", null));
    }

    @PostMapping("/api/v1/library/workouts/{workoutId}/schedules")
    @Operation(summary = "Schedule a workout for a client on a specific date")
    public ResponseEntity<ApiResponse<ScheduleResponse>> schedule(
            @PathVariable UUID workoutId,
            @Valid @RequestBody ScheduleWorkoutRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workout scheduled", assignmentService.schedule(workoutId, coachId, req)));
    }

    @DeleteMapping("/api/v1/library/schedules/{scheduleId}")
    @Operation(summary = "Soft-delete a scheduled workout entry")
    public ResponseEntity<ApiResponse<Void>> unschedule(@PathVariable UUID scheduleId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        assignmentService.unschedule(scheduleId, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Schedule removed", null));
    }

    @GetMapping("/api/v1/library/clients/{clientId}/schedules")
    @Operation(summary = "List a client's scheduled workouts (optionally filter by date range)")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> listClientSchedules(
            @PathVariable UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(assignmentService.listSchedules(clientId, coachId, from, to)));
    }
}
