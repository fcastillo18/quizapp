package com.fsl.quizapp.attempt.dto;

import java.util.List;
import java.util.UUID;

/** Response DTO for a single question within a quiz attempt. Omits explanation. */
public record AttemptQuestionResponse(UUID id, String text, int position,
    List<AttemptOptionResponse> options) {
}
