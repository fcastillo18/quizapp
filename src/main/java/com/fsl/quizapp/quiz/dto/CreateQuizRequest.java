package com.fsl.quizapp.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Request DTO for creating a new quiz with its questions and options. */
public record CreateQuizRequest(
    @NotBlank String title,
    String description,
    @NotEmpty @Valid List<CreateQuestionRequest> questions) {
}
