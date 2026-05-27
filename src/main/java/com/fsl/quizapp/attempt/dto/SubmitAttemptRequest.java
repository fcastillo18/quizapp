package com.fsl.quizapp.attempt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request body for the submit-attempt endpoint.
 *
 * @param userId  the UUID of the learner submitting the quiz
 * @param answers the list of answers — must contain at least one entry
 */
public record SubmitAttemptRequest(
    @NotNull UUID userId,
    @NotEmpty @Valid List<SubmitAnswerRequest> answers) {
}
