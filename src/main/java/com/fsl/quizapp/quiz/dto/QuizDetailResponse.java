package com.fsl.quizapp.quiz.dto;

import java.util.List;
import java.util.UUID;

/** Full detail response for a quiz, including questions and options ordered by position. */
public record QuizDetailResponse(
    UUID id,
    String title,
    String description,
    List<QuestionResponse> questions) {
}
