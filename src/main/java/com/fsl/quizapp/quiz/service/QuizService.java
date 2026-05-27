package com.fsl.quizapp.quiz.service;

import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.quiz.dto.OptionResponse;
import com.fsl.quizapp.quiz.dto.QuestionResponse;
import com.fsl.quizapp.quiz.dto.QuizDetailResponse;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.entity.Question;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import java.util.Comparator;
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

  /**
   * Returns full quiz detail including questions and options, sorted by position ascending.
   *
   * @param id the quiz UUID
   * @return quiz detail with questions and options
   * @throws ResourceNotFoundException if no quiz exists with the given id
   */
  public QuizDetailResponse getQuizDetails(UUID id) {
    return quizRepository.findByIdWithQuestionsAndOptions(id)
        .map(quiz -> {
          List<QuestionResponse> questions = quiz.getQuestions().stream()
              .sorted(Comparator.comparingInt(Question::getPosition))
              .map(q -> {
                List<OptionResponse> options = q.getOptions().stream()
                    .sorted(Comparator.comparingInt(
                        com.fsl.quizapp.quiz.entity.Option::getPosition))
                    .map(o -> new OptionResponse(o.getId(), o.getText(), o.getPosition()))
                    .toList();
                return new QuestionResponse(q.getId(), q.getText(), q.getPosition(), options);
              })
              .toList();
          return new QuizDetailResponse(
              quiz.getId(), quiz.getTitle(), quiz.getDescription(), questions);
        })
        .orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
  }
}
