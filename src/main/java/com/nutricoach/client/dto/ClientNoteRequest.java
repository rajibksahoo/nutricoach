package com.nutricoach.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientNoteRequest(@NotBlank @Size(max = 5000) String body) {}
