package com.fsl.quizapp.attempt.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Response DTO returned when a new quiz attempt is started. */
public record AttemptStartResponse(
    UUID attemptId,
    UUID quizId,
    UUID userId,
    OffsetDateTime startedAt,
    List<AttemptQuestionResponse> questions) {
}
