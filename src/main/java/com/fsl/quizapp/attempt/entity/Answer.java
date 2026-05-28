package com.fsl.quizapp.attempt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing a single answer submitted as part of a quiz attempt. */
@Entity
@Table(name = "answers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

  /** Primary key, auto-generated as UUID. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** The attempt this answer belongs to. */
  @Column(name = "attempt_id", nullable = false)
  private UUID attemptId;

  /** The question being answered. */
  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  /** The option selected by the learner. */
  @Column(name = "selected_option_id", nullable = false)
  private UUID selectedOptionId;

  /** Whether the selected option was the correct answer. */
  @Column(nullable = false)
  private boolean correct;
}
