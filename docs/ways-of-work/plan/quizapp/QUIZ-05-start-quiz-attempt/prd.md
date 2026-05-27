# QUIZ-05 — Start Quiz Attempt

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01 — Database Schema & Seed Data](../QUIZ-01-database-schema-seed-data/prd.md)  
**Required by:** [QUIZ-06](../QUIZ-06-submit-answers-scoring/prd.md), [QUIZ-08](../QUIZ-08-unit-integration-test-suite/prd.md), [QUIZ-09](../QUIZ-09-user-attempt-history/prd.md), [QUIZ-10](../QUIZ-10-detailed-attempt-results/prd.md), [QUIZ-11](../QUIZ-11-user-aggregate-statistics/prd.md)

---

## Goal

**Problem:** There is no mechanism to begin a quiz session. Without an attempt record, answers cannot be submitted and progress cannot be tracked per user per quiz.

**Solution:** A `POST /quizzes/{quizId}/attempts` endpoint that creates an attempt record, links it to a user and quiz, records the start time, and returns the full question+options list ready for answering.

**Impact:** Opens the quiz-taking session. Every subsequent submit endpoint depends on the attempt ID returned here.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Initiates a quiz session before answering questions. |

---

## User Stories

- As a learner, I want to start a new quiz attempt so that I have a session to submit my answers against.
- As a learner, I want to receive the questions and options in the start response so that I do not need a second API call before answering.
- As a learner, I want to be able to start multiple attempts for the same quiz so that I can retake it and improve my score.

---

## Requirements

### Functional Requirements

- `POST /quizzes/{quizId}/attempts` accepts a JSON body: `{ "userId": "<UUID>" }`.
- `userId` is required and must be non-blank.
- Creates an `attempt` record with:
  - `user_id` from the request body.
  - `quiz_id` from the path parameter.
  - `started_at` set to the current UTC timestamp.
  - `submitted_at`, `score`, `percentage` left null (attempt is open).
- Returns HTTP 201 with:
  ```json
  {
    "attemptId": "<UUID>",
    "quizId": "<UUID>",
    "userId": "<UUID>",
    "startedAt": "<ISO 8601>",
    "questions": [
      {
        "id": "<UUID>",
        "text": "string",
        "position": 1,
        "options": [
          { "id": "<UUID>", "text": "string", "position": 1 }
        ]
      }
    ]
  }
  ```
- `correctOptionId` and `explanation` are never present in the response.
- A user may start multiple attempts for the same quiz; each is a distinct `attempt` row with its own `id`.
- Returns HTTP 404 if `quizId` does not exist.
- Returns HTTP 400 if `userId` is missing or blank.
- `Location: /attempts/{attemptId}` header is included in the 201 response.

### Non-Functional Requirements

- Attempt creation and question loading happen within a single read-then-write operation; no orphan attempt rows on failure.
- Endpoint documented in SpringDoc OpenAPI with 201, 400, and 404 response examples.
- Questions ordered by `position` ascending; options ordered by `position` ascending.
- Security: `correctOptionId` and `explanation` must never appear in the response — verified by an integration test asserting `assertThat(responseBody).doesNotContain("correctOptionId")`.
- HTTP 201 response includes a `Location` header pointing to the created resource (`/attempts/{attemptId}`).
- All timestamps in responses use ISO 8601 UTC format (e.g., `2026-05-27T14:00:00Z`).
- Error responses use the standard shape: `{ "status": <code>, "message": "<human-readable>", "timestamp": "<ISO>" }` (applicable to HTTP 400 and 404 cases).

---

## Acceptance Criteria

- [ ] `POST /quizzes/{validId}/attempts` with a valid `userId` returns HTTP 201.
- [ ] Response contains `attemptId`, `quizId`, `userId`, `startedAt`, and `questions` array.
- [ ] Each question in the response has `id`, `text`, `position`, and `options`.
- [ ] Each option has `id`, `text`, `position` and no other fields.
- [ ] `correctOptionId` and `explanation` are absent from the response.
- [ ] A new row exists in the `attempts` table with `submitted_at = NULL`.
- [ ] `Location` header points to `/attempts/{attemptId}`.
- [ ] Calling the endpoint twice for the same `quizId` + `userId` creates two distinct attempt rows.
- [ ] `POST /quizzes/nonexistent-id/attempts` returns HTTP 404.
- [ ] Missing `userId` in the body returns HTTP 400.
- [ ] Swagger UI documents 201, 400, and 404.

---

## Out of Scope

- Resuming an in-progress attempt (clients re-call `GET /quizzes/{quizId}` if they need questions again).
- Time-limiting an attempt.
- Locking a quiz once an attempt is in progress.
