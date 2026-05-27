package com.fsl.quizapp.attempt.dto;

import java.util.UUID;

/**
 * Per-question result returned as part of the submit-attempt response.
 *
 * @param questionId  the UUID of the question
 * @param correct     whether the selected option was correct
 * @param explanation the explanation shown after submission
 */
public record QuestionResultResponse(
    UUID questionId,
    boolean correct,
    String explanation) {
}
