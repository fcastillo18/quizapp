package com.fsl.quizapp.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Request DTO for creating a single answer option. */
public record CreateOptionRequest(
    @NotBlank String text,
    @Min(1) int position) {
}
