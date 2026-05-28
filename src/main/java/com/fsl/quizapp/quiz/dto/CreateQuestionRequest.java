package com.fsl.quizapp.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Request DTO for creating a question with its options. */
public record CreateQuestionRequest(
    @NotBlank String text,
    @NotBlank String explanation,
    @Min(1) int position,
    @Size(min = 2) @Valid List<CreateOptionRequest> options,
    @Min(1) int correctOptionPosition) {
}
