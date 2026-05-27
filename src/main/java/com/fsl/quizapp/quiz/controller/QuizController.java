package com.fsl.quizapp.quiz.controller;

import com.fsl.quizapp.quiz.dto.QuizDetailResponse;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.service.QuizService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for quiz endpoints. */
@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

  private final QuizService quizService;

  /**
   * Returns a summary list of all available quizzes.
   *
   * @return 200 with list of quiz summaries
   */
  @GetMapping
  public ResponseEntity<List<QuizSummaryResponse>> listQuizzes() {
    return ResponseEntity.ok(quizService.listQuizzes());
  }

  /**
   * Returns full quiz detail including questions and options, sorted by position.
   *
   * @param id the quiz UUID
   * @return 200 with quiz detail or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<QuizDetailResponse> getQuizDetails(@PathVariable UUID id) {
    return ResponseEntity.ok(quizService.getQuizDetails(id));
  }
}
