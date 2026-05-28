package com.fsl.quizapp.quiz.repository;

import com.fsl.quizapp.quiz.entity.Quiz;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for Quiz entities. */
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

  /**
   * Fetches a quiz with its questions eagerly to avoid N+1 selects.
   * Options are batch-loaded via {@code @BatchSize} on the options collection.
   *
   * @param id the quiz UUID
   * @return the quiz with questions loaded, or empty if not found
   */
  @Query(
      "SELECT DISTINCT q FROM Quiz q"
          + " LEFT JOIN FETCH q.questions"
          + " WHERE q.id = :id")
  Optional<Quiz> findByIdWithQuestionsAndOptions(@Param("id") UUID id);
}
