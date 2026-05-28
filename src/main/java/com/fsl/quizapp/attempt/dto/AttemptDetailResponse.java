package com.fsl.quizapp.attempt.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full breakdown of a submitted quiz attempt.
 *
 * @param attemptId      the UUID of the attempt
 * @param userId         the UUID of the user who took the quiz
 * @param quizId         the UUID of the quiz
 * @param quizTitle      the title of the quiz
 * @param startedAt      when the attempt was started
 * @param submittedAt    when the attempt was submitted
 * @param score          the number of correct answers
 * @param totalQuestions the total number of questions in the quiz
 * @param percentage     the score as a percentage rounded to two decimal places
 * @param results        per-question breakdown ordered by question position ascending
 */
public record AttemptDetailResponse(
    UUID attemptId,
    UUID userId,
    UUID quizId,
    String quizTitle,
    OffsetDateTime startedAt,
    OffsetDateTime submittedAt,
    int score,
    int totalQuestions,
    BigDecimal percentage,
    List<QuestionDetailResult> results) {
}
