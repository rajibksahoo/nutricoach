package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.CreateExerciseRequest;
import com.nutricoach.library.dto.ExerciseResponse;
import com.nutricoach.library.dto.UpdateExerciseRequest;
import com.nutricoach.library.service.ExerciseService;
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
@Tag(name = "Library — Exercises", description = "Coach-owned exercise library")
@RequestMapping("/api/v1/library/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create an exercise")
    public ResponseEntity<ApiResponse<ExerciseResponse>> create(@Valid @RequestBody CreateExerciseRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Exercise created", exerciseService.create(coachId, req)));
    }

    @GetMapping
    @Operation(summary = "List all exercises for the current coach")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> list() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(exerciseService.list(coachId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an exercise")
    public ResponseEntity<ApiResponse<ExerciseResponse>> get(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(exerciseService.get(id, coachId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exercise")
    public ResponseEntity<ApiResponse<ExerciseResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateExerciseRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Exercise updated", exerciseService.update(id, coachId, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exercise (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        exerciseService.delete(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Exercise deleted", null));
    }
}
