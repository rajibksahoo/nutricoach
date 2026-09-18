package com.nutricoach.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientNoteResponse(UUID id, String body, Instant createdAt, Instant updatedAt) {}
