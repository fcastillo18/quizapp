package com.fsl.quizapp.attempt.controller;

import com.fsl.quizapp.attempt.dto.AttemptDetailResponse;
import com.fsl.quizapp.attempt.dto.AttemptStartResponse;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptResponse;
import com.fsl.quizapp.attempt.service.AttemptService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** REST controller for quiz attempt operations. */
@RestController
@RequiredArgsConstructor
public class AttemptController {

  private final AttemptService attemptService;

  /**
   * Starts a new quiz attempt for the given user.
   *
   * @param quizId     the UUID of the quiz to attempt
   * @param request    the request body containing the userId
   * @param uriBuilder builder used to construct the Location header
   * @return 201 Created with the attempt details and Location header
   */
  @PostMapping("/quizzes/{quizId}/attempts")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<AttemptStartResponse> startAttempt(
      @PathVariable UUID quizId,
      @Valid @RequestBody StartAttemptRequest request,
      UriComponentsBuilder uriBuilder) {
    AttemptStartResponse response = attemptService.startAttempt(quizId, request);
    URI location = uriBuilder.path("/attempts/{id}").buildAndExpand(response.attemptId()).toUri();
    return ResponseEntity.created(location).body(response);
  }

  /**
   * Submits answers for an in-progress attempt and returns the scoring result.
   *
   * @param attemptId the UUID of the attempt to submit
   * @param request   the request body containing userId and all answers
   * @return 200 OK with score, percentage, feedback, and per-question results
   */
  @PostMapping("/attempts/{attemptId}/submit")
  public ResponseEntity<SubmitAttemptResponse> submitAttempt(
      @PathVariable UUID attemptId,
      @Valid @RequestBody SubmitAttemptRequest request) {
    return ResponseEntity.ok(attemptService.submitAttempt(attemptId, request));
  }

  /**
   * Returns the full per-question breakdown of a submitted attempt.
   *
   * @param attemptId the UUID of the attempt to retrieve
   * @return 200 OK with score, percentage, and per-question results
   */
  @GetMapping("/attempts/{attemptId}")
  public ResponseEntity<AttemptDetailResponse> getAttemptDetail(@PathVariable UUID attemptId) {
    return ResponseEntity.ok(attemptService.getAttemptDetail(attemptId));
  }
}
