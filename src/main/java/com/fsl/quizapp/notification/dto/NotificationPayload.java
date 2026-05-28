package com.fsl.quizapp.notification.dto;

/**
 * Immutable data object carrying all information needed to send a quiz result email.
 *
 * <p>Note: {@code userEmail} is present for delivery purposes only and must never be written
 * to logs.
 */
public record NotificationPayload(
    String userName,
    String userEmail,
    String quizTitle,
    String score,
    double percentage,
    String feedbackMessage,
    String completedAt) {}
