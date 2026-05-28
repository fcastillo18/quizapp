package com.fsl.quizapp.notification;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fsl.quizapp.notification.dto.NotificationPayload;
import com.fsl.quizapp.notification.entity.Notification;
import com.fsl.quizapp.notification.entity.NotificationStatus;
import com.fsl.quizapp.notification.repository.NotificationRepository;
import com.fsl.quizapp.user.entity.User;
import com.fsl.quizapp.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link NotificationService}. No Spring context is loaded. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private EmailService emailService;

  private NotificationService service;

  private UUID notificationId;
  private UUID userId;
  private Notification notification;
  private User user;

  /** Sets up common test fixtures before each test. */
  @BeforeEach
  void setUp() {
    service = new NotificationService(notificationRepository, userRepository, emailService);

    notificationId = UUID.randomUUID();
    userId = UUID.randomUUID();

    notification = Notification.builder()
        .id(notificationId)
        .attemptId(UUID.randomUUID())
        .status(NotificationStatus.PENDING)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();

    user = User.builder()
        .id(userId)
        .name("Alice")
        .email("alice@example.com")
        .createdAt(OffsetDateTime.now())
        .build();
  }

  /**
   * Normal dispatch: EmailService is called exactly once and the notification status transitions
   * to SENT.
   */
  @Test
  void sendResultEmail_success_marksNotificationSent() {
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    service.sendResultEmail(
        notificationId, userId, "Java Basics", 4, 5, 80.0, "Great job!", OffsetDateTime.now());

    verify(emailService).sendResultsEmail(any(NotificationPayload.class));

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus())
        .isEqualTo(NotificationStatus.SENT);
  }

  /**
   * Failure path: when EmailService throws, the exception must not propagate to the caller and
   * the notification status must transition to FAILED.
   */
  @Test
  void sendResultEmail_emailServiceThrows_marksNotificationFailed() {
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    org.mockito.Mockito.doThrow(new RuntimeException("SMTP error"))
        .when(emailService).sendResultsEmail(any(NotificationPayload.class));

    assertThatNoException().isThrownBy(() ->
        service.sendResultEmail(
            notificationId, userId, "Java Basics", 4, 5, 80.0, "Great job!",
            OffsetDateTime.now()));

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getStatus())
        .isEqualTo(NotificationStatus.FAILED);
  }
}
