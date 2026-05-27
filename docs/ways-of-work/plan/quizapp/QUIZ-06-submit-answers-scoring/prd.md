# QUIZ-06 — Submit Quiz Answers with Scoring & Feedback

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01](../QUIZ-01-database-schema-seed-data/prd.md), [QUIZ-05](../QUIZ-05-start-quiz-attempt/prd.md), [QUIZ-07](../QUIZ-07-async-email-notification/prd.md)  
**Required by:** [QUIZ-08](../QUIZ-08-unit-integration-test-suite/prd.md), [QUIZ-09](../QUIZ-09-user-attempt-history/prd.md), [QUIZ-10](../QUIZ-10-detailed-attempt-results/prd.md), [QUIZ-11](../QUIZ-11-user-aggregate-statistics/prd.md)

---

## Goal

**Problem:** Learners complete questions but have no way to submit answers, receive a score, or understand which answers were right or wrong.

**Solution:** A `POST /attempts/{attemptId}/submit` endpoint that accepts all answers at once, scores them using a pure `ScoringService`, persists the result, returns contextual feedback, and asynchronously triggers the email notification.

**Impact:** Core value delivery. Learners receive immediate, question-level feedback. Downstream tickets (QUIZ-09, QUIZ-10) read from the persisted result.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Submits answers and receives their score, feedback, and per-question explanations. |

---

## User Stories

- As a learner, I want to submit all my answers at once so that the quiz is scored and I receive my result immediately.
- As a learner, I want contextual feedback based on my score so that I know how well I performed.
- As a learner, I want per-question feedback showing which answers were correct and the explanation so that I can learn from mistakes.
- As a learner, I want my submission to succeed even if the email notification fails so that my result is never lost.

---

## Requirements

### Functional Requirements

**Endpoint:**
- `POST /attempts/{attemptId}/submit` accepts:
  ```json
  {
    "userId": "<UUID>",
    "answers": [
      { "questionId": "<UUID>", "selectedOptionId": "<UUID>" }
    ]
  }
  ```

**Validation:**
- `userId` must be non-blank.
- `answers` must not be null or empty.
- Returns HTTP 404 if `attemptId` does not exist.
- Returns HTTP 409 if the attempt already has a `submitted_at` value (already submitted).

**Scoring (`ScoringService` — pure, no I/O):**
- Compare each `selectedOptionId` to `questions.correct_option_id`.
- `score` = count of correct answers.
- `percentage` = `(score / total questions) * 100`, rounded to two decimal places.

**Feedback message thresholds:**
- `percentage >= 80` → encouraging message, e.g., `"Great work! Keep it up!"`
- `60 <= percentage < 80` → motivational message, e.g., `"Good effort! Review the missed topics."`
- `percentage < 60` → improvement-focused message, e.g., `"Keep practicing — you'll get there!"`

**Persistence (all in one `@Transactional` block):**
- Insert one row in `answers` per submitted answer with `correct` boolean set.
- Update the `attempt` row: set `submitted_at` (UTC now), `score`, `percentage`.
- Insert a `notifications` row with `status = PENDING`.

**Async notification (after transaction commits):**
- Invoke `NotificationService.sendResultsEmail(attemptId)` via `@Async`.
- `NotificationService` catches all exceptions, updates `notifications.status` to `FAILED`, and logs the error — it never throws to the caller.
- On success, `NotificationService` sets `notifications.status = SENT`.

**Response (HTTP 200):**
```json
{
  "attemptId": "<UUID>",
  "score": 4,
  "totalQuestions": 5,
  "percentage": 80.00,
  "feedbackMessage": "Great work! Keep it up!",
  "results": [
    {
      "questionId": "<UUID>",
      "correct": true,
      "explanation": "string"
    }
  ]
}
```

### Non-Functional Requirements

- `ScoringService` has no Spring dependencies — instantiable with `new ScoringService()` in unit tests.
- The async notification must not be awaited before returning the HTTP response.
- `answers` table rows and `attempt` update are written atomically; notification dispatch happens outside the main transaction.
- Endpoint documented in SpringDoc OpenAPI with 200, 400, 404, and 409 response examples.

---

## Acceptance Criteria

- [ ] `POST /attempts/{validId}/submit` with all correct answers returns HTTP 200 with `percentage: 100.00` and an encouraging message.
- [ ] Partial answers (e.g., 3/5 correct) return the correct score, percentage, and motivational or improvement message as appropriate.
- [ ] Each element in `results` contains `questionId`, `correct`, and `explanation`.
- [ ] The `attempt` row has `submitted_at`, `score`, and `percentage` set after the call.
- [ ] A `notifications` row exists for the attempt after submission.
- [ ] Submitting the same attempt twice returns HTTP 409.
- [ ] `POST /attempts/nonexistent-id/submit` returns HTTP 404.
- [ ] If `NotificationService` throws, the submission still returns HTTP 200 and the attempt is persisted; notification `status = FAILED`.
- [ ] `ScoringService` is tested without Spring context: all correct → 100%, all wrong → 0%, 3/5 → 60.00%, boundary cases at exactly 60% and 80%.
- [ ] Feedback message boundary tested: 79.99% → motivational, 80.00% → encouraging, 59.99% → improvement.

---

## Out of Scope

- Partial submissions (submitting a subset of questions and resuming later).
- Re-scoring an already-submitted attempt.
- Sending real email (handled by mock — see QUIZ-07).
- Returning detailed progress statistics (see QUIZ-09, QUIZ-10, QUIZ-11).
