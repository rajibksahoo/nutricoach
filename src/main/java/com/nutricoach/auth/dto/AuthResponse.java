package com.nutricoach.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID coachId,
        String phone,
        boolean isNewCoach
) {}
