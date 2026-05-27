package com.fsl.quizapp.quiz.service;

import com.fsl.quizapp.common.exception.BadRequestException;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.quiz.dto.CreateOptionRequest;
import com.fsl.quizapp.quiz.dto.CreateQuestionRequest;
import com.fsl.quizapp.quiz.dto.CreateQuizRequest;
import com.fsl.quizapp.quiz.dto.CreatedQuizResponse;
import com.fsl.quizapp.quiz.dto.OptionResponse;
import com.fsl.quizapp.quiz.dto.QuestionResponse;
import com.fsl.quizapp.quiz.dto.QuizDetailResponse;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.entity.Option;
import com.fsl.quizapp.quiz.entity.Question;
import com.fsl.quizapp.quiz.entity.Quiz;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   * Creates a new quiz with all its questions and options in a single transaction.
   * Sets {@code correctOptionId} on each question after options are flushed.
   *
   * @param request the quiz creation payload
   * @return the ID of the newly created quiz
   * @throws BadRequestException if {@code correctOptionPosition} is out of range for any question
   */
  @Transactional
  public CreatedQuizResponse createQuiz(CreateQuizRequest request) {
    Quiz quiz = Quiz.builder()
        .title(request.title())
        .description(request.description())
        .build();
    quizRepository.save(quiz);

    for (CreateQuestionRequest questionReq : request.questions()) {
      if (questionReq.correctOptionPosition() < 1
          || questionReq.correctOptionPosition() > questionReq.options().size()) {
        throw new BadRequestException(
            "correctOptionPosition " + questionReq.correctOptionPosition()
            + " is out of range for question: " + questionReq.text());
      }
      Question question = Question.builder()
          .quiz(quiz)
          .text(questionReq.text())
          .explanation(questionReq.explanation())
          .position(questionReq.position())
          .build();
      for (CreateOptionRequest optionReq : questionReq.options()) {
        Option option = Option.builder()
            .question(question)
            .text(optionReq.text())
            .position(optionReq.position())
            .build();
        question.getOptions().add(option);
      }
      quiz.getQuestions().add(question);
      // Flush to assign IDs to the new options before setting correctOptionId
      quizRepository.flush();
      Option correctOption = question.getOptions().get(questionReq.correctOptionPosition() - 1);
      question.setCorrectOptionId(correctOption.getId());
    }
    quizRepository.save(quiz);
    return new CreatedQuizResponse(quiz.getId());
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
