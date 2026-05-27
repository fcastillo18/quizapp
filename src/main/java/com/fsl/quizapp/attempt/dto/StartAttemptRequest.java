package com.fsl.quizapp.attempt.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request body for starting a new quiz attempt. */
public record StartAttemptRequest(
    @NotNull UUID userId) {
}
