package com.fsl.quizapp.quiz.controller;

import com.fsl.quizapp.quiz.dto.CreateQuizRequest;
import com.fsl.quizapp.quiz.dto.CreatedQuizResponse;
import com.fsl.quizapp.quiz.dto.QuizDetailResponse;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.service.QuizService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** REST controller for quiz endpoints. */
@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

  private final QuizService quizService;

  /**
   * Creates a new quiz with questions and options.
   *
   * @param request the quiz creation payload
   * @param uriBuilder used to build the Location header
   * @return 201 with the new quiz ID and a Location header pointing to the created resource
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<CreatedQuizResponse> createQuiz(
      @Valid @RequestBody CreateQuizRequest request,
      UriComponentsBuilder uriBuilder) {
    CreatedQuizResponse response = quizService.createQuiz(request);
    URI location = uriBuilder.path("/quizzes/{id}").buildAndExpand(response.id()).toUri();
    return ResponseEntity.created(location).body(response);
  }

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
