package com.fsl.quizapp.progress;

import com.fsl.quizapp.progress.dto.AttemptSummaryResponse;
import com.fsl.quizapp.progress.dto.UserStatsResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service providing user progress data including attempt history and aggregate statistics. */
@Service
@RequiredArgsConstructor
public class ProgressService {

  private final ProgressRepository progressRepository;

  /**
   * Returns all attempts for the given user ordered by startedAt descending.
   * Returns an empty list if the user has no attempts.
   *
   * @param userId the user's UUID
   * @return list of attempt summaries, never null
   */
  public List<AttemptSummaryResponse> getUserAttempts(UUID userId) {
    return progressRepository.findAttemptSummariesByUserId(userId);
  }

  /**
   * Returns aggregate statistics for submitted attempts of the given user.
   * If the user has no submitted attempts, totalAttempts is 0 and averageScore is 0.00.
   *
   * @param userId the user's UUID
   * @return user statistics including total submitted attempts and average score
   */
  public UserStatsResponse getUserStats(UUID userId) {
    Object[] row = progressRepository.findStatsByUserId(userId).get(0);
    long totalAttempts = ((Number) row[0]).longValue();
    Object avg = row[1];
    BigDecimal averageScore;
    if (avg == null) {
      averageScore = BigDecimal.ZERO;
    } else if (avg instanceof BigDecimal bd) {
      averageScore = bd;
    } else {
      averageScore = BigDecimal.valueOf(((Number) avg).doubleValue());
    }
    return new UserStatsResponse(userId, totalAttempts,
        averageScore.setScale(2, RoundingMode.HALF_UP));
  }
}
