package com.nutricoach.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content must not be blank")
        @Size(max = 2000, message = "Message must be 2000 characters or fewer")
        String content
) {}
