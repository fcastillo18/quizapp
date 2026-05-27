package com.fsl.quizapp.attempt.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Represents a single answer submitted for one question within a quiz attempt.
 *
 * @param questionId       the UUID of the question being answered
 * @param selectedOptionId the UUID of the option selected by the learner
 */
public record SubmitAnswerRequest(
    @NotNull UUID questionId,
    @NotNull UUID selectedOptionId) {
}
