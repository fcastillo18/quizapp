package com.fsl.quizapp.quiz.service;

import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service handling quiz business logic. */
@Service
@RequiredArgsConstructor
public class QuizService {

  private final QuizRepository quizRepository;

  /**
   * Returns a summary list of all quizzes (id, title, description only).
   *
   * @return list of quiz summaries
   */
  public List<QuizSummaryResponse> listQuizzes() {
    return quizRepository.findAll().stream()
        .map(q -> new QuizSummaryResponse(q.getId(), q.getTitle(), q.getDescription()))
        .toList();
  }

  /**
   * Returns the quiz for the given id or throws ResourceNotFoundException if absent.
   *
   * @param id the quiz UUID
   * @return the quiz summary
   */
  public QuizSummaryResponse getQuizSummary(UUID id) {
    return quizRepository.findById(id)
        .map(q -> new QuizSummaryResponse(q.getId(), q.getTitle(), q.getDescription()))
        .orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
  }
}
