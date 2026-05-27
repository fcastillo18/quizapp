package com.fsl.quizapp.attempt.service;

import com.fsl.quizapp.attempt.dto.AttemptOptionResponse;
import com.fsl.quizapp.attempt.dto.AttemptQuestionResponse;
import com.fsl.quizapp.attempt.dto.AttemptStartResponse;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.entity.Attempt;
import com.fsl.quizapp.attempt.repository.AttemptRepository;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.quiz.entity.Question;
import com.fsl.quizapp.quiz.entity.Quiz;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service handling quiz attempt creation. */
@Service
@RequiredArgsConstructor
public class AttemptService {

  private final AttemptRepository attemptRepository;
  private final QuizRepository quizRepository;

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
}
