package com.fsl.quizapp.quiz.dto;

import java.util.UUID;

/** Response DTO for a single answer option — never exposes whether it is the correct option. */
public record OptionResponse(UUID id, String text, int position) {
}
