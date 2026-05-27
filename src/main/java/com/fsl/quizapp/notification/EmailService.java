package com.fsl.quizapp.notification;

import com.fsl.quizapp.notification.dto.NotificationPayload;

/** Contract for sending quiz result emails to learners. */
public interface EmailService {

  /**
   * Sends a quiz result notification email.
   *
   * @param payload all data needed to compose and deliver the email
   */
  void sendResultsEmail(NotificationPayload payload);
}
