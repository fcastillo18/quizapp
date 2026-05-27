package com.fsl.quizapp.progress.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary of a single quiz attempt, used in the user attempt history list.
 *
 * @param attemptId   the UUID of the attempt
 * @param quizId      the UUID of the quiz
 * @param quizTitle   the title of the quiz
 * @param startedAt   when the attempt was started
 * @param submittedAt when the attempt was submitted, or null if still open
 * @param score       number of correct answers, or null if not yet submitted
 * @param percentage  score as a percentage rounded to two decimal places, or null if open
 */
public record AttemptSummaryResponse(
    UUID attemptId,
    UUID quizId,
    String quizTitle,
    OffsetDateTime startedAt,
    OffsetDateTime submittedAt,
    Integer score,
    BigDecimal percentage) {
}
