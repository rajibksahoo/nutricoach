package com.nutricoach.auth.dto;

import java.util.UUID;

public record ClientAuthResponse(
        String token,
        UUID clientId,
        UUID coachId,
        String name,
        String phone
) {}
