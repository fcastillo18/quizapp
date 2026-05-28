package com.fsl.quizapp.attempt.repository;

import com.fsl.quizapp.attempt.entity.Answer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link Answer} entities. */
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

  /**
   * Finds all answers submitted for a given attempt.
   *
   * @param attemptId the attempt UUID
   * @return list of answers for the attempt
   */
  List<Answer> findByAttemptId(UUID attemptId);
}
