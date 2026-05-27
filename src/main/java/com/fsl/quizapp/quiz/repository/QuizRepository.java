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
   * Fetches a quiz with its questions and options in one query to avoid N+1 selects.
   *
   * @param id the quiz UUID
   * @return an Optional containing the quiz with all associations loaded, or empty if not found
   */
  @Query(
      "SELECT DISTINCT q FROM Quiz q"
          + " LEFT JOIN FETCH q.questions qn"
          + " LEFT JOIN FETCH qn.options"
          + " WHERE q.id = :id"
          + " ORDER BY qn.position ASC")
  Optional<Quiz> findByIdWithQuestionsAndOptions(@Param("id") UUID id);
}
