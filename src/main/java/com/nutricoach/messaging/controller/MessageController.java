package com.nutricoach.messaging.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.messaging.dto.ConversationSummaryResponse;
import com.nutricoach.messaging.dto.MessageResponse;
import com.nutricoach.messaging.dto.SendMessageRequest;
import com.nutricoach.messaging.service.MessageService;
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
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Messages", description = "In-app messaging between coach and clients")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtils securityUtils;

    @GetMapping("/conversations")
    @Operation(summary = "List all conversations", description = "Returns one summary per client thread, sorted by most recent message")
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> listConversations() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(messageService.getConversations(coachId)));
    }

    @GetMapping("/clients/{clientId}")
    @Operation(summary = "Get message thread", description = "Returns all messages for a client conversation (oldest first). Also marks unread messages as read.")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(messageService.getMessages(coachId, clientId)));
    }

    @PostMapping("/clients/{clientId}")
    @Operation(summary = "Send a message", description = "Send a message from the coach to a client")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID clientId,
            @Valid @RequestBody SendMessageRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        MessageResponse msg = messageService.sendMessage(coachId, clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Message sent", msg));
    }
}
