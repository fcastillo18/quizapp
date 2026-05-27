# QUIZ-10 — Detailed Attempt Results

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01](../QUIZ-01-database-schema-seed-data/prd.md), [QUIZ-05](../QUIZ-05-start-quiz-attempt/prd.md), [QUIZ-06](../QUIZ-06-submit-answers-scoring/prd.md)  
**Required by:** — (none)

**Phase:** 2

---

## Goal

**Problem:** After completing a quiz, learners can only see the submission response once. If they want to review their answers and explanations later, there is no endpoint to retrieve that data.

**Solution:** A `GET /attempts/{attemptId}` endpoint that returns the full breakdown of a submitted attempt: overall score, and per-question correctness with the selected option, correct option, and explanation.

**Impact:** Enables post-attempt learning review. Learners can revisit any past attempt to understand where they went wrong.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Reviews a past attempt to study missed questions and explanations. |

---

## User Stories

- As a learner, I want to retrieve the full results of a past attempt so that I can review what I got right and wrong.
- As a learner, I want to see the explanation for each question so that I can learn from my mistakes.

---

## Requirements

### Functional Requirements

- `GET /attempts/{attemptId}` returns HTTP 200 with the full attempt detail.
- Response structure:
  ```json
  {
    "attemptId": "<UUID>",
    "userId": "<UUID>",
    "quizId": "<UUID>",
    "quizTitle": "string",
    "startedAt": "<ISO 8601>",
    "submittedAt": "<ISO 8601>",
    "score": 4,
    "totalQuestions": 5,
    "percentage": 80.00,
    "results": [
      {
        "questionId": "<UUID>",
        "questionText": "string",
        "selectedOptionId": "<UUID>",
        "selectedOptionText": "string",
        "correctOptionId": "<UUID>",
        "correctOptionText": "string",
        "correct": true,
        "explanation": "string"
      }
    ]
  }
  ```
- `results` is ordered by question `position` ascending.
- Returns HTTP 404 if `attemptId` does not exist.
- Returns HTTP 404 if the attempt exists but has no `submitted_at` (not yet submitted — no results to show).
- `correctOptionId` and `correctOptionText` ARE present in this response (the learner has already submitted — knowing the answer is now appropriate).
- Implemented via `AttemptController` → `AttemptService` (or `ProgressService`) → repositories.
- All data loaded via JOIN queries — no N+1 lazy loading.

### Non-Functional Requirements

- Response must include both `selectedOptionText` and `correctOptionText` to avoid requiring the client to look up option text separately.
- Endpoint documented in SpringDoc OpenAPI with 200 and 404 response examples.
- All timestamps in responses use ISO 8601 UTC format (e.g., `2026-05-27T14:00:00Z`).
- Error responses use the standard shape: `{ "status": <code>, "message": "<human-readable>", "timestamp": "<ISO>" }` (applicable to HTTP 404 cases).

---

## Acceptance Criteria

- [ ] `GET /attempts/{validSubmittedId}` returns HTTP 200.
- [ ] Response contains `attemptId`, `userId`, `quizId`, `quizTitle`, `startedAt`, `submittedAt`, `score`, `totalQuestions`, `percentage`.
- [ ] `results` array contains one entry per question, each with `questionId`, `questionText`, `selectedOptionId`, `selectedOptionText`, `correctOptionId`, `correctOptionText`, `correct`, `explanation`.
- [ ] `correct` is `true` for questions the learner answered correctly and `false` otherwise.
- [ ] `results` are ordered by question `position` ascending.
- [ ] `GET /attempts/nonexistent-id` returns HTTP 404.
- [ ] `GET /attempts/{openAttemptId}` (not yet submitted) returns HTTP 404.
- [ ] Swagger UI documents 200 and 404 responses with full schema.

---

## Testing Requirements

Integration tests (`@SpringBootTest`, `@ActiveProfiles("test")`, `MockMvc`):

| Scenario | Expected |
|---|---|
| `GET /attempts/{validSubmittedId}` for a completed attempt | HTTP 200; response contains all fields including `results` array with per-question breakdown |
| `results` correctness: correct and incorrect answers reflected accurately | Each entry has `correct = true/false` matching actual submission |
| `GET /attempts/nonexistent-id` | HTTP 404 with standard error body |
| `GET /attempts/{openAttemptId}` (attempt not yet submitted) | HTTP 404 with standard error body |
| `results` ordered by question `position` ascending | First result entry has the lowest `position` value |
| `correctOptionId` and `correctOptionText` present after submission | Response includes these fields (post-submission disclosure is valid) |

---

## Out of Scope

- Editing or resubmitting a past attempt.
- Filtering results by correct/incorrect.
- Streaming or partial responses for large quizzes.
