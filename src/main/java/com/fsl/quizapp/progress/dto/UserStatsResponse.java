package com.fsl.quizapp.progress.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregate statistics for a user's submitted quiz attempts.
 *
 * @param userId         the user's UUID
 * @param totalAttempts  number of submitted attempts (submittedAt IS NOT NULL)
 * @param averageScore   mean percentage across submitted attempts, rounded to 2 decimal places;
 *                       0.00 if no submitted attempts
 */
public record UserStatsResponse(UUID userId, long totalAttempts, BigDecimal averageScore) {
}
