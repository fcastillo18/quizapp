package com.fsl.quizapp.attempt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fsl.quizapp.attempt.service.ScoringService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ScoringService} — no Spring context required. */
class ScoringServiceTest {

  private final ScoringService scoringService = new ScoringService();

  /** All answers correct — expects 100.00%. */
  @Test
  void calculatePercentage_allCorrect_returnsOneHundred() {
    BigDecimal result = scoringService.calculatePercentage(5, 5);
    assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  /** No correct answers — expects 0.00%. */
  @Test
  void calculatePercentage_allWrong_returnsZero() {
    BigDecimal result = scoringService.calculatePercentage(0, 5);
    assertThat(result).isEqualByComparingTo(new BigDecimal("0.00"));
  }

  /** 3 out of 5 correct — expects 60.00%. */
  @Test
  void calculatePercentage_threeOfFiveCorrect_returnsSixty() {
    BigDecimal result = scoringService.calculatePercentage(3, 5);
    assertThat(result).isEqualByComparingTo(new BigDecimal("60.00"));
  }

  /** 4 out of 5 correct — expects 80.00%. */
  @Test
  void calculatePercentage_fourOfFiveCorrect_returnsEighty() {
    BigDecimal result = scoringService.calculatePercentage(4, 5);
    assertThat(result).isEqualByComparingTo(new BigDecimal("80.00"));
  }

  /** 100% score — expects encouraging feedback. */
  @Test
  void getFeedbackMessage_perfectScore_returnsEncouraging() {
    String msg = scoringService.getFeedbackMessage(new BigDecimal("100.00"));
    assertThat(msg).isEqualTo("Excellent work! Keep it up!");
  }

  /** Exactly 80% — boundary inclusive for encouraging tier. */
  @Test
  void getFeedbackMessage_eightyPercent_returnsEncouraging() {
    String msg = scoringService.getFeedbackMessage(new BigDecimal("80.00"));
    assertThat(msg).isEqualTo("Excellent work! Keep it up!");
  }

  /** 79% — just below encouraging threshold, expects motivational message. */
  @Test
  void getFeedbackMessage_seventyNinePercent_returnsMotivational() {
    String msg = scoringService.getFeedbackMessage(new BigDecimal("79.00"));
    assertThat(msg).isEqualTo("Good effort! Review the topics you missed.");
  }

  /** Exactly 60% — boundary inclusive for motivational tier. */
  @Test
  void getFeedbackMessage_sixtyPercent_returnsMotivational() {
    String msg = scoringService.getFeedbackMessage(new BigDecimal("60.00"));
    assertThat(msg).isEqualTo("Good effort! Review the topics you missed.");
  }

  /** 59% — just below motivational threshold, expects improvement message. */
  @Test
  void getFeedbackMessage_fiftyNinePercent_returnsImprovement() {
    String msg = scoringService.getFeedbackMessage(new BigDecimal("59.00"));
    assertThat(msg).isEqualTo("Keep practicing! Focus on the areas where you struggled.");
  }

  /** 0% score — expects improvement-focused message. */
  @Test
  void getFeedbackMessage_zeroPercent_returnsImprovement() {
    String msg = scoringService.getFeedbackMessage(new BigDecimal("0.00"));
    assertThat(msg).isEqualTo("Keep practicing! Focus on the areas where you struggled.");
  }
}
