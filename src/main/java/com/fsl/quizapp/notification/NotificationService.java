package com.fsl.quizapp.notification;

import com.fsl.quizapp.notification.dto.NotificationPayload;
import com.fsl.quizapp.notification.entity.Notification;
import com.fsl.quizapp.notification.entity.NotificationStatus;
import com.fsl.quizapp.notification.repository.NotificationRepository;
import com.fsl.quizapp.user.entity.User;
import com.fsl.quizapp.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous service responsible for sending quiz result email notifications.
 *
 * <p>This service is always called after a submission transaction has committed. It never throws
 * — any failure is caught, logged, and recorded by transitioning the notification status to
 * {@link NotificationStatus#FAILED}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;

  /**
   * Sends a quiz result email asynchronously after a submission. Never throws — failures are
   * logged and recorded.
   *
   * @param notificationId  the ID of the pre-created {@link Notification} record
   * @param userId          the ID of the learner who submitted the quiz
   * @param quizTitle       title of the quiz that was submitted
   * @param score           number of correct answers
   * @param totalQuestions  total number of questions in the quiz
   * @param percentage      percentage score (0.0 – 100.0)
   * @param feedbackMessage human-readable feedback based on the result
   * @param completedAt     the instant the submission was processed
   */
  @Async("notificationExecutor")
  public void sendResultEmail(
      UUID notificationId,
      UUID userId,
      String quizTitle,
      int score,
      int totalQuestions,
      double percentage,
      String feedbackMessage,
      OffsetDateTime completedAt) {
    Notification notification = notificationRepository.findById(notificationId).orElse(null);
    if (notification == null) {
      log.error("Notification {} not found — skipping", notificationId);
      return;
    }
    try {
      User user = userRepository.findById(userId).orElse(null);
      String userName = user != null ? user.getName() : "Learner";
      String userEmail = user != null ? user.getEmail() : "unknown@example.com";
      String scoreStr = score + "/" + totalQuestions;
      String completedAtStr = completedAt.withOffsetSameInstant(ZoneOffset.UTC).toString();
      NotificationPayload payload = new NotificationPayload(
          userName, userEmail, quizTitle, scoreStr, percentage, feedbackMessage, completedAtStr);
      emailService.sendResultsEmail(payload);
      notification.setStatus(NotificationStatus.SENT);
      notification.setUpdatedAt(OffsetDateTime.now());
      notificationRepository.save(notification);
    } catch (Exception ex) {
      log.error("Failed to send notification {}: {}", notificationId, ex.getMessage(), ex);
      notification.setStatus(NotificationStatus.FAILED);
      notification.setUpdatedAt(OffsetDateTime.now());
      notificationRepository.save(notification);
    }
  }
}
