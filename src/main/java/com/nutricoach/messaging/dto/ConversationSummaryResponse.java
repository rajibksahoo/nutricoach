package com.nutricoach.messaging.dto;

import com.nutricoach.messaging.entity.Message;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID clientId,
        String clientName,
        String clientPhone,
        String lastMessage,
        Message.SenderType lastSenderType,
        Instant lastMessageAt,
        long unreadCount
) {}
