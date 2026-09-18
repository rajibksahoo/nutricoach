package com.nutricoach.client.controller;

import com.nutricoach.client.dto.ClientActivityResponse;
import com.nutricoach.client.dto.ClientNoteRequest;
import com.nutricoach.client.dto.ClientNoteResponse;
import com.nutricoach.client.dto.ClientTrainingStatsResponse;
import com.nutricoach.client.service.ClientDetailService;
import com.nutricoach.client.service.ClientNoteService;
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

import java.util.List;
import java.util.UUID;

/** The cards on the client-detail screen: Training, Updates and Notes. */
@RestController
@RequestMapping("/api/v1/clients/{clientId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client detail", description = "Training stats, activity feed and coach notes for one client")
public class ClientDetailController {

    private final ClientDetailService clientDetailService;
    private final ClientNoteService clientNoteService;
    private final SecurityUtils securityUtils;

    @GetMapping("/training-stats")
    @Operation(summary = "Training volume for a client",
            description = "Planned vs completed workouts over the last 7 and 30 days, what is planned for the next week, and the most recent completion")
    public ResponseEntity<ApiResponse<ClientTrainingStatsResponse>> trainingStats(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(clientDetailService.getTrainingStats(clientId, coachId)));
    }

    @GetMapping("/activity")
    @Operation(summary = "Recent activity for a client",
            description = "Messages, check-ins, progress logs, completed workouts and the join event, newest first")
    public ResponseEntity<ApiResponse<List<ClientActivityResponse>>> activity(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(clientDetailService.getActivity(clientId, coachId)));
    }

    @GetMapping("/notes")
    @Operation(summary = "List coach notes on a client")
    public ResponseEntity<ApiResponse<List<ClientNoteResponse>>> listNotes(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(clientNoteService.list(clientId, coachId)));
    }

    @PostMapping("/notes")
    @Operation(summary = "Add a note to a client")
    public ResponseEntity<ApiResponse<ClientNoteResponse>> createNote(
            @PathVariable UUID clientId, @Valid @RequestBody ClientNoteRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Note added", clientNoteService.create(clientId, coachId, req)));
    }

    @PutMapping("/notes/{noteId}")
    @Operation(summary = "Edit a note")
    public ResponseEntity<ApiResponse<ClientNoteResponse>> updateNote(
            @PathVariable UUID clientId, @PathVariable UUID noteId,
            @Valid @RequestBody ClientNoteRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Note updated",
                clientNoteService.update(clientId, noteId, coachId, req)));
    }

    @DeleteMapping("/notes/{noteId}")
    @Operation(summary = "Delete a note")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID clientId, @PathVariable UUID noteId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        clientNoteService.delete(clientId, noteId, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Note deleted", null));
    }
}
