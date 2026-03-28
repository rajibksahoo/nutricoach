package com.nutricoach.ai.controller;

import com.nutricoach.ai.dto.AiJobResponse;
import com.nutricoach.ai.dto.GenerateMealPlanRequest;
import com.nutricoach.ai.entity.AiJob;
import com.nutricoach.ai.repository.AiJobRepository;
import com.nutricoach.ai.service.AiMealPlanService;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "AI", description = "AI-powered meal plan generation")
@SecurityRequirement(name = "bearerAuth")
public class AiJobController {

    private final AiMealPlanService aiMealPlanService;
    private final AiJobRepository aiJobRepository;
    private final SecurityUtils securityUtils;

    @PostMapping("/meal-plans/generate")
    @Operation(summary = "Generate meal plan with AI",
               description = "Submits an async GPT-4o job to generate a 7-day Indian meal plan for the given client. Poll GET /api/v1/ai/jobs/{id} for status.")
    public ResponseEntity<ApiResponse<AiJobResponse>> generate(
            @Valid @RequestBody GenerateMealPlanRequest request) {

        UUID coachId = securityUtils.getCurrentCoachId();
        AiJob job = aiMealPlanService.createJob(coachId, request.clientId());
        aiMealPlanService.processJob(job.getId());  // @Async — returns immediately
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Meal plan generation started", toResponse(job)));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Poll AI job status",
               description = "Returns current status of an AI job. When COMPLETED, generatedMealPlanId is populated.")
    public ResponseEntity<ApiResponse<AiJobResponse>> getJob(@PathVariable UUID jobId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        AiJob job = aiJobRepository.findByIdAndCoachId(jobId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("AI job not found"));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(job)));
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private static AiJobResponse toResponse(AiJob job) {
        UUID generatedMealPlanId = null;
        if (job.getStatus() == AiJob.Status.COMPLETED && job.getOutputPayload() != null) {
            Object raw = job.getOutputPayload().get("mealPlanId");
            if (raw != null) {
                generatedMealPlanId = UUID.fromString(raw.toString());
            }
        }
        return new AiJobResponse(
                job.getId(),
                job.getClientId(),
                job.getStatus().name(),
                job.getJobType().name(),
                job.getCreatedAt(),
                job.getCompletedAt(),
                job.getErrorMessage(),
                generatedMealPlanId
        );
    }
}
