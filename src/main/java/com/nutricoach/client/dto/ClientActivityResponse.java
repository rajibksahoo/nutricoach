package com.nutricoach.client.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One entry in a client's Updates feed.
 *
 * <p>Derived at read time by unioning messages, check-ins, progress logs,
 * workout completions and the join event — there is no activity_log table.
 * That keeps every existing write path untouched, at the cost of a handful of
 * small queries per read.
 *
 * @param type MESSAGE | CHECK_IN | PROGRESS_LOG | WORKOUT_DONE | CLIENT_JOINED
 */
public record ClientActivityResponse(
        String type,
        UUID clientId,
        String summary,
        Instant occurredAt) {}
