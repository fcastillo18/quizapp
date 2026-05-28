package com.fsl.quizapp.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fsl.quizapp.attempt.dto.AttemptStartResponse;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.entity.Attempt;
import com.fsl.quizapp.attempt.repository.AnswerRepository;
import com.fsl.quizapp.attempt.repository.AttemptRepository;
import com.fsl.quizapp.attempt.service.AttemptService;
import com.fsl.quizapp.attempt.service.ScoringService;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.notification.NotificationService;
import com.fsl.quizapp.notification.repository.NotificationRepository;
import com.fsl.quizapp.quiz.entity.Option;
import com.fsl.quizapp.quiz.entity.Question;
import com.fsl.quizapp.quiz.entity.Quiz;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link AttemptService}. */
@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

  @Mock
  private AttemptRepository attemptRepository;

  @Mock
  private QuizRepository quizRepository;

  @Mock
  private AnswerRepository answerRepository;

  @Mock
  private ScoringService scoringService;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private AttemptService attemptService;

  /** Verifies that a valid request saves an attempt and returns the expected response. */
  @Test
  void startAttempt_validRequest_returnsResponse() {
    UUID quizId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();

    Option option = Option.builder()
        .id(UUID.randomUUID())
        .text("Option A")
        .position(1)
        .build();

    Question question = Question.builder()
        .id(UUID.randomUUID())
        .text("What is LLM?")
        .position(1)
        .options(List.of(option))
        .build();

    Quiz quiz = Quiz.builder()
        .id(quizId)
        .title("LLM Fundamentals")
        .questions(List.of(question))
        .build();

    Attempt savedAttempt = Attempt.builder()
        .id(attemptId)
        .userId(userId)
        .quiz(quiz)
        .startedAt(OffsetDateTime.now())
        .build();

    when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
    when(attemptRepository.save(any(Attempt.class))).thenReturn(savedAttempt);

    AttemptStartResponse response =
        attemptService.startAttempt(quizId, new StartAttemptRequest(userId));

    assertThat(response.attemptId()).isEqualTo(attemptId);
    assertThat(response.quizId()).isEqualTo(quizId);
    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.startedAt()).isNotNull();
    assertThat(response.questions()).hasSize(1);
    assertThat(response.questions().get(0).options()).hasSize(1);

    verify(attemptRepository).save(any(Attempt.class));
  }

  /** Verifies that an unknown quiz ID throws {@link ResourceNotFoundException}. */
  @Test
  void startAttempt_unknownQuizId_throwsResourceNotFoundException() {
    UUID quizId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(quizRepository.findById(quizId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> attemptService.startAttempt(quizId, new StartAttemptRequest(userId)))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
