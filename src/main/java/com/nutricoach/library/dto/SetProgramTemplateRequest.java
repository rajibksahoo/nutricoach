package com.nutricoach.library.dto;

import jakarta.validation.constraints.NotNull;

/** Promote a program to a reusable template, or demote it back. */
public record SetProgramTemplateRequest(@NotNull Boolean isTemplate) {}
