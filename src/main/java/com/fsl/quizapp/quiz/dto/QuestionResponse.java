package com.fsl.quizapp.quiz.dto;

import java.util.List;
import java.util.UUID;

/** Response DTO for a quiz question — never exposes correctOptionId or explanation. */
public record QuestionResponse(UUID id, String text, int position, List<OptionResponse> options) {
}
