package com.nutricoach.library.dto;

public record ProgramCoverUploadResponse(
        String uploadUrl,
        String s3Key) {}
