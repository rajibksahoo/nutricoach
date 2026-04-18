package com.nutricoach.messaging.dto;

import com.nutricoach.messaging.entity.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID clientId,
        Message.SenderType senderType,
        String content,
        boolean read,
        Instant sentAt
) {}
