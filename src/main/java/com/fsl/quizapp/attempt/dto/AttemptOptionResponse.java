package com.fsl.quizapp.attempt.dto;

import java.util.UUID;

/** Response DTO for a single option within a quiz attempt question. Omits correctOptionId. */
public record AttemptOptionResponse(UUID id, String text, int position) {
}
