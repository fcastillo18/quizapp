package com.fsl.quizapp.attempt.dto;

import java.util.UUID;

/**
 * Per-question breakdown returned by the attempt detail endpoint.
 *
 * @param questionId         the UUID of the question
 * @param questionText       the text of the question
 * @param selectedOptionId   the UUID of the option selected by the learner
 * @param selectedOptionText the text of the selected option
 * @param correctOptionId    the UUID of the correct option
 * @param correctOptionText  the text of the correct option
 * @param correct            whether the selected option was the correct answer
 * @param explanation        the explanation for the correct answer
 */
public record QuestionDetailResult(
    UUID questionId,
    String questionText,
    UUID selectedOptionId,
    String selectedOptionText,
    UUID correctOptionId,
    String correctOptionText,
    boolean correct,
    String explanation) {
}
