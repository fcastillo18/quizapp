package com.fsl.quizapp.attempt.service;

import com.fsl.quizapp.attempt.dto.AttemptOptionResponse;
import com.fsl.quizapp.attempt.dto.AttemptQuestionResponse;
import com.fsl.quizapp.attempt.dto.AttemptStartResponse;
import com.fsl.quizapp.attempt.dto.QuestionResultResponse;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAnswerRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptResponse;
import com.fsl.quizapp.attempt.entity.Answer;
import com.fsl.quizapp.attempt.entity.Attempt;
import com.fsl.quizapp.attempt.repository.AnswerRepository;
import com.fsl.quizapp.attempt.repository.AttemptRepository;
import com.fsl.quizapp.common.exception.ConflictException;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.notification.NotificationService;
import com.fsl.quizapp.notification.entity.Notification;
import com.fsl.quizapp.notification.entity.NotificationStatus;
import com.fsl.quizapp.notification.repository.NotificationRepository;
import com.fsl.quizapp.quiz.entity.Question;
import com.fsl.quizapp.quiz.entity.Quiz;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service handling quiz attempt creation and submission. */
@Service
@RequiredArgsConstructor
public class AttemptService {

  private final AttemptRepository attemptRepository;
  private final QuizRepository quizRepository;
  private final AnswerRepository answerRepository;
  private final ScoringService scoringService;
  private final NotificationRepository notificationRepository;
  private final NotificationService notificationService;

  /**
   * Starts a new quiz attempt for the given user and quiz.
   *
   * @param quizId  the UUID of the quiz to attempt
   * @param request the request containing the userId
   * @return the created attempt response with questions and options
   * @throws ResourceNotFoundException if the quiz does not exist
   */
  @Transactional
  public AttemptStartResponse startAttempt(UUID quizId, StartAttemptRequest request) {
    Quiz quiz = quizRepository.findById(quizId)
        .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId));

    Attempt attempt = Attempt.builder()
        .userId(request.userId())
        .quiz(quiz)
        .startedAt(OffsetDateTime.now())
        .build();
    attempt = attemptRepository.save(attempt);

    List<AttemptQuestionResponse> questions = quiz.getQuestions().stream()
        .sorted(Comparator.comparingInt(Question::getPosition))
        .map(q -> new AttemptQuestionResponse(
            q.getId(), q.getText(), q.getPosition(),
            q.getOptions().stream()
                .sorted(Comparator.comparingInt(com.fsl.quizapp.quiz.entity.Option::getPosition))
                .map(o -> new AttemptOptionResponse(o.getId(), o.getText(), o.getPosition()))
                .toList()))
        .toList();

    return new AttemptStartResponse(
        attempt.getId(), quiz.getId(), request.userId(), attempt.getStartedAt(), questions);
  }

  /**
   * Submits answers for an in-progress attempt, scores them, persists the result, and
   * asynchronously triggers an email notification.
   *
   * @param attemptId the UUID of the attempt to submit
   * @param request   the submission request containing userId and all answers
   * @return the scoring result with per-question feedback
   * @throws ResourceNotFoundException if the attemptId does not exist
   * @throws ConflictException         if the attempt has already been submitted
   */
  @Transactional
  public SubmitAttemptResponse submitAttempt(UUID attemptId, SubmitAttemptRequest request) {
    Attempt attempt = attemptRepository.findById(attemptId)
        .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));
    if (attempt.getSubmittedAt() != null) {
      throw new ConflictException("Attempt already submitted: " + attemptId);
    }

    Quiz quiz = quizRepository.findByIdWithQuestionsAndOptions(attempt.getQuiz().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Quiz", attempt.getQuiz().getId()));

    Map<UUID, UUID> questionCorrectOption = quiz.getQuestions().stream()
        .collect(Collectors.toMap(Question::getId, Question::getCorrectOptionId));
    Map<UUID, String> questionExplanation = quiz.getQuestions().stream()
        .collect(Collectors.toMap(Question::getId, Question::getExplanation));

    int totalQuestions = quiz.getQuestions().size();
    int score = 0;
    List<QuestionResultResponse> results = new ArrayList<>();
    List<Answer> answers = new ArrayList<>();

    for (SubmitAnswerRequest ar : request.answers()) {
      UUID correctOptionId = questionCorrectOption.get(ar.questionId());
      boolean isCorrect = ar.selectedOptionId().equals(correctOptionId);
      if (isCorrect) {
        score++;
      }
      answers.add(Answer.builder()
          .attemptId(attemptId)
          .questionId(ar.questionId())
          .selectedOptionId(ar.selectedOptionId())
          .correct(isCorrect)
          .build());
      results.add(new QuestionResultResponse(
          ar.questionId(), isCorrect, questionExplanation.get(ar.questionId())));
    }

    answerRepository.saveAll(answers);

    BigDecimal percentage = scoringService.calculatePercentage(score, totalQuestions);

    attempt.setScore(score);
    attempt.setPercentage(percentage);
    attempt.setSubmittedAt(OffsetDateTime.now());
    attemptRepository.save(attempt);

    Notification notification = Notification.builder()
        .attemptId(attemptId)
        .status(NotificationStatus.PENDING)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
    notification = notificationRepository.save(notification);

    String feedback = scoringService.getFeedbackMessage(percentage);
    notificationService.sendResultEmail(
        notification.getId(), request.userId(), quiz.getTitle(),
        score, totalQuestions, percentage.doubleValue(), feedback,
        attempt.getSubmittedAt());

    return new SubmitAttemptResponse(score, totalQuestions, percentage, feedback, results);
  }
}
