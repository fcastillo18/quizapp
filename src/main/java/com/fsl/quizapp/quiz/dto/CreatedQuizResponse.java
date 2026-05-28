package com.fsl.quizapp.quiz.dto;

import java.util.UUID;

/** Response DTO returned when a quiz is successfully created — contains only the new quiz ID. */
public record CreatedQuizResponse(UUID id) {
}
