package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachSectionRequest(
        @NotNull UUID sectionId,
        Integer position) {}
