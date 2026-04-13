package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.service.WorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Library — Workouts", description = "Coach-owned workouts and reusable sections")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final SecurityUtils securityUtils;

    // ── Workouts ──────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/library/workouts")
    @Operation(summary = "Create a workout")
    public ResponseEntity<ApiResponse<WorkoutSummaryResponse>> create(@Valid @RequestBody CreateWorkoutRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workout created", workoutService.create(coachId, req)));
    }

    @GetMapping("/api/v1/library/workouts")
    @Operation(summary = "List all workouts for the current coach")
    public ResponseEntity<ApiResponse<List<WorkoutSummaryResponse>>> list() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(workoutService.list(coachId)));
    }

    @GetMapping("/api/v1/library/workouts/{id}")
    @Operation(summary = "Get a workout with its ordered sections")
    public ResponseEntity<ApiResponse<WorkoutResponse>> get(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(workoutService.get(id, coachId)));
    }

    @PutMapping("/api/v1/library/workouts/{id}")
    @Operation(summary = "Update workout metadata")
    public ResponseEntity<ApiResponse<WorkoutSummaryResponse>> update(@PathVariable UUID id,
                                                                      @Valid @RequestBody UpdateWorkoutRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Workout updated", workoutService.update(id, coachId, req)));
    }

    @DeleteMapping("/api/v1/library/workouts/{id}")
    @Operation(summary = "Delete a workout (soft delete; detaches all sections)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        workoutService.delete(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Workout deleted", null));
    }

    @PostMapping("/api/v1/library/workouts/{id}/sections")
    @Operation(summary = "Attach an existing section to a workout")
    public ResponseEntity<ApiResponse<WorkoutResponse>> attachSection(@PathVariable UUID id,
                                                                      @Valid @RequestBody AttachSectionRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Section attached", workoutService.attachSection(id, coachId, req)));
    }

    @DeleteMapping("/api/v1/library/workouts/{id}/sections/{assignmentId}")
    @Operation(summary = "Detach a section from a workout (section itself is preserved)")
    public ResponseEntity<ApiResponse<Void>> detachSection(@PathVariable UUID id,
                                                           @PathVariable UUID assignmentId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        workoutService.detachSection(id, coachId, assignmentId);
        return ResponseEntity.ok(ApiResponse.ok("Section detached", null));
    }

    // ── Sections (first-class library entity) ─────────────────────────────────

    @PostMapping("/api/v1/library/workout-sections")
    @Operation(summary = "Create a reusable workout section")
    public ResponseEntity<ApiResponse<WorkoutSectionResponse>> createSection(
            @Valid @RequestBody CreateWorkoutSectionRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Section created", workoutService.createSection(coachId, req)));
    }

    @GetMapping("/api/v1/library/workout-sections")
    @Operation(summary = "List all workout sections")
    public ResponseEntity<ApiResponse<List<WorkoutSectionResponse>>> listSections() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(workoutService.listSections(coachId)));
    }

    @GetMapping("/api/v1/library/workout-sections/{id}")
    @Operation(summary = "Get a workout section with its exercises")
    public ResponseEntity<ApiResponse<WorkoutSectionResponse>> getSection(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(workoutService.getSection(id, coachId)));
    }

    @PutMapping("/api/v1/library/workout-sections/{id}")
    @Operation(summary = "Update section metadata")
    public ResponseEntity<ApiResponse<WorkoutSectionResponse>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkoutSectionRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Section updated", workoutService.updateSection(id, coachId, req)));
    }

    @DeleteMapping("/api/v1/library/workout-sections/{id}")
    @Operation(summary = "Delete a section (fails if any workout still uses it)")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        workoutService.deleteSection(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Section deleted", null));
    }

    @PostMapping("/api/v1/library/workout-sections/{id}/exercises")
    @Operation(summary = "Add an exercise entry to a section")
    public ResponseEntity<ApiResponse<WorkoutSectionResponse>> addSectionExercise(
            @PathVariable UUID id,
            @Valid @RequestBody AddSectionExerciseRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Exercise added", workoutService.addSectionExercise(id, coachId, req)));
    }

    @DeleteMapping("/api/v1/library/workout-sections/{id}/exercises/{entryId}")
    @Operation(summary = "Remove an exercise entry from a section")
    public ResponseEntity<ApiResponse<Void>> removeSectionExercise(@PathVariable UUID id,
                                                                   @PathVariable UUID entryId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        workoutService.removeSectionExercise(id, coachId, entryId);
        return ResponseEntity.ok(ApiResponse.ok("Exercise removed", null));
    }
}
