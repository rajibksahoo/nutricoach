package com.nutricoach.progress.dto;

import java.time.Instant;
import java.util.UUID;

public record PhotoResponse(UUID id, String photoType, String downloadUrl, Instant createdAt) {
}
