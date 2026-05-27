package com.fsl.quizapp.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing an email notification for a quiz attempt result. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  /** Primary key, auto-generated as UUID. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** The attempt this notification is associated with. */
  @Column(name = "attempt_id", nullable = false)
  private UUID attemptId;

  /** Current delivery status of the notification. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationStatus status;

  /** Timestamp when the notification was created. Never updated after insert. */
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  /** Timestamp when the notification record was last updated. */
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
