package com.nutricoach.progress.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One progress photo in the client-wide grid, carrying the date of the log it
 * belongs to so the UI can label and group by date without a second lookup.
 *
 * <p>Distinct from {@link PhotoResponse}, which is scoped to a single log.
 * springdoc keys OpenAPI components by simple name, so the two must not share
 * one — a collision silently drops a schema from the generated client.
 *
 * <p>{@code downloadUrl} is a pre-signed S3 URL valid for 60 minutes; consumers
 * must cope with it expiring.
 */
public record ClientPhotoResponse(
        UUID id,
        UUID progressLogId,
        LocalDate loggedDate,
        String photoType,
        String downloadUrl,
        Instant createdAt) {}
