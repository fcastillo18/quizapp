package com.fsl.quizapp.user.controller;

import com.fsl.quizapp.progress.ProgressService;
import com.fsl.quizapp.progress.dto.AttemptSummaryResponse;
import com.fsl.quizapp.progress.dto.UserStatsResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for user-specific endpoints including attempt history and statistics. */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final ProgressService progressService;

  /**
   * Returns all quiz attempts for the given user, ordered by most recent first.
   * Returns an empty array if the user has no attempts (never 404).
   *
   * @param userId the user's UUID
   * @return 200 with list of attempt summaries
   */
  @GetMapping("/{userId}/attempts")
  public ResponseEntity<List<AttemptSummaryResponse>> getUserAttempts(
      @PathVariable UUID userId) {
    return ResponseEntity.ok(progressService.getUserAttempts(userId));
  }

  /**
   * Returns aggregate statistics for submitted attempts of the given user.
   * Returns zero totals if the user has no submitted attempts (never 404).
   *
   * @param userId the user's UUID
   * @return 200 with user statistics
   */
  @GetMapping("/{userId}/stats")
  public ResponseEntity<UserStatsResponse> getUserStats(
      @PathVariable UUID userId) {
    return ResponseEntity.ok(progressService.getUserStats(userId));
  }
}
