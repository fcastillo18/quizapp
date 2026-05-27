package com.fsl.quizapp.progress;

import com.fsl.quizapp.attempt.entity.Attempt;
import com.fsl.quizapp.progress.dto.AttemptSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA repository for progress-related queries on {@link Attempt} entities. */
public interface ProgressRepository extends JpaRepository<Attempt, UUID> {

  /**
   * Returns a summary of all attempts for the given user, most recent first.
   * Fetches quiz title via implicit join to avoid N+1 queries.
   *
   * @param userId the user's UUID
   * @return list of attempt summaries ordered by startedAt descending
   */
  @Query("SELECT new com.fsl.quizapp.progress.dto.AttemptSummaryResponse("
      + "a.id, a.quiz.id, a.quiz.title, a.startedAt, a.submittedAt, a.score, a.percentage) "
      + "FROM Attempt a WHERE a.userId = :userId ORDER BY a.startedAt DESC")
  List<AttemptSummaryResponse> findAttemptSummariesByUserId(@Param("userId") UUID userId);

  /**
   * Returns aggregate statistics for submitted attempts of the given user.
   * Result is a single {@code Object[]} where index 0 is COUNT (Long) and
   * index 1 is AVG percentage (Double/BigDecimal), using COALESCE to avoid null when no rows.
   *
   * @param userId the user's UUID
   * @return single-element array containing COUNT and AVG percentage
   */
  @Query("SELECT COUNT(a), COALESCE(AVG(a.percentage), 0) "
      + "FROM Attempt a WHERE a.userId = :userId AND a.submittedAt IS NOT NULL")
  Object[] findStatsByUserId(@Param("userId") UUID userId);
}
