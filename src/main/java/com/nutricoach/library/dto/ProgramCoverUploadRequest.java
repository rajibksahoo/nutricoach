package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotBlank;

public record ProgramCoverUploadRequest(
        @NotBlank String contentType) {}
