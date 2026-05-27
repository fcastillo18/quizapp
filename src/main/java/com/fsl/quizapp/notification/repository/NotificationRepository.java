package com.fsl.quizapp.notification.repository;

import com.fsl.quizapp.notification.entity.Notification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link Notification}. */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** Finds a notification by its associated attempt identifier. */
  Optional<Notification> findByAttemptId(UUID attemptId);
}
