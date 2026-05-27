package com.fsl.quizapp.attempt.repository;

import com.fsl.quizapp.attempt.entity.Attempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link Attempt} entities. */
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

  /**
   * Finds all attempts by a given user, most recent first.
   *
   * @param userId the user's UUID
   * @return list of attempts ordered by startedAt descending
   */
  List<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId);
}
