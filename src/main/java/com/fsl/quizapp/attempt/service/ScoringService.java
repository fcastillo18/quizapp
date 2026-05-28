package com.fsl.quizapp.attempt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** Pure scoring service that calculates quiz scores and feedback messages with no I/O. */
@Service
public class ScoringService {

  /**
   * Calculates the percentage score rounded to 2 decimal places.
   *
   * @param score the number of correct answers
   * @param total the total number of questions
   * @return the percentage as a BigDecimal with scale 2, or ZERO if total is 0
   */
  public BigDecimal calculatePercentage(int score, int total) {
    if (total == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(score)
        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Returns the feedback message based on the percentage score.
   *
   * <ul>
   *   <li>{@code >= 80%} — encouraging message</li>
   *   <li>{@code >= 60%} — motivational message</li>
   *   <li>{@code < 60%} — improvement-focused message</li>
   * </ul>
   *
   * @param percentage the score percentage (0.00 – 100.00)
   * @return the feedback message string
   */
  public String getFeedbackMessage(BigDecimal percentage) {
    if (percentage.compareTo(new BigDecimal("80.00")) >= 0) {
      return "Excellent work! Keep it up!";
    } else if (percentage.compareTo(new BigDecimal("60.00")) >= 0) {
      return "Good effort! Review the topics you missed.";
    } else {
      return "Keep practicing! Focus on the areas where you struggled.";
    }
  }
}
