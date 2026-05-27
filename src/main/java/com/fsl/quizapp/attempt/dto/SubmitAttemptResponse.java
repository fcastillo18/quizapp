package com.fsl.quizapp.attempt.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response returned by the submit-attempt endpoint.
 *
 * @param score           the number of correct answers
 * @param totalQuestions  the total number of questions in the quiz
 * @param percentage      the score as a percentage rounded to two decimal places
 * @param feedbackMessage contextual feedback based on the score percentage
 * @param results         per-question results including correctness and explanation
 */
public record SubmitAttemptResponse(
    int score,
    int totalQuestions,
    BigDecimal percentage,
    String feedbackMessage,
    List<QuestionResultResponse> results) {
}
