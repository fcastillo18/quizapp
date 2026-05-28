package com.fsl.quizapp.quiz.dto;

import java.util.UUID;

/** Summary representation of a quiz, excluding questions and answers. */
public record QuizSummaryResponse(UUID id, String title, String description) {
}
