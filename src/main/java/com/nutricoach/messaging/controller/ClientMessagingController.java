package com.nutricoach.messaging.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
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
@RequestMapping("/api/v1/portal/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client Portal — Messages", description = "In-app chat between client and their coach")
@SecurityRequirement(name = "bearerAuth")
public class ClientMessagingController {

    private final MessageService messageService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get chat thread", description = "Returns all messages with the coach (oldest first). Also marks coach messages as read.")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages() {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(messageService.getMessagesForClient(clientId, coachId)));
    }

    @PostMapping
    @Operation(summary = "Send a message", description = "Send a message from the client to their coach")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request) {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        MessageResponse msg = messageService.sendMessageFromClient(clientId, coachId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Message sent", msg));
    }
}
