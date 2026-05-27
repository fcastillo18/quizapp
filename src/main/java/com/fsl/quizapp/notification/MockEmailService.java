package com.fsl.quizapp.notification;

import com.fsl.quizapp.notification.dto.NotificationPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mock {@link EmailService} implementation that simulates email delivery with a configurable
 * delay. Intended for development and testing only.
 */
@Slf4j
@Service
public class MockEmailService implements EmailService {

  /** Milliseconds to sleep in order to simulate email sending latency. */
  @Value("${notification.mock.delay-ms:100}")
  private long delayMs;

  /**
   * Simulates sending a quiz result email by sleeping for {@code delayMs} milliseconds and
   * logging the delivery. The user email address is intentionally omitted from logs (PII rule).
   *
   * @param payload all data needed to compose the mock email
   * @throws RuntimeException if the sleeping thread is interrupted
   */
  @Override
  public void sendResultsEmail(NotificationPayload payload) {
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Mock email sending interrupted", ex);
    }
    log.info("Mock email sent to user '{}' for quiz '{}'",
        payload.userName(), payload.quizTitle());
  }
}
