package com.nutricoach.library.dto;

import jakarta.validation.constraints.Size;

/**
 * Start a new program from an existing one (typically a template).
 *
 * @param name optional name for the copy; defaults to "{source} (copy)"
 */
public record InstantiateProgramRequest(@Size(max = 150) String name) {}
