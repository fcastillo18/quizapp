# QUIZ-11 — User Aggregate Statistics

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01](../QUIZ-01-database-schema-seed-data/prd.md), [QUIZ-06](../QUIZ-06-submit-answers-scoring/prd.md)

**Phase:** 2

---

## Goal

**Problem:** Learners have no single-number view of their overall progress. They must manually infer their average score by browsing individual attempt records.

**Solution:** A `GET /users/{userId}/stats` endpoint that returns two aggregate values: total completed attempts and average score across all submitted attempts.

**Impact:** Gives learners an instant snapshot of their overall performance. Lightweight to implement — a single aggregate SQL query. Provides the data foundation for a future progress dashboard.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Checks overall quiz performance at a glance. |

---

## User Stories

- As a learner, I want to see how many quizzes I have completed so that I know my level of engagement.
- As a learner, I want to see my average score so that I understand my overall performance across all attempts.

---

## Requirements

### Functional Requirements

- `GET /users/{userId}/stats` returns HTTP 200.
- Response structure:
  ```json
  {
    "userId": "<UUID>",
    "totalAttempts": 5,
    "averageScore": 74.40
  }
  ```
- `totalAttempts` counts only **submitted** attempts (`submitted_at IS NOT NULL`).
- `averageScore` is the mean of `percentage` across all submitted attempts, rounded to two decimal places.
- If the user has no submitted attempts, returns:
  ```json
  { "userId": "<UUID>", "totalAttempts": 0, "averageScore": 0.00 }
  ```
- The aggregate is computed in a single repository query (e.g., JPQL with `COUNT` and `AVG`) — not computed in Java by iterating attempts.
- Implemented via `UserController` → `ProgressService` → `AttemptRepository`.

### Non-Functional Requirements

- Never returns HTTP 404 for a `userId` — if the user exists but has no completed attempts, return zeroed values. If `userId` format is invalid (malformed UUID), return HTTP 400.
- Endpoint documented in SpringDoc OpenAPI with a 200 response example.
- Query executes in a single round trip to the database.

---

## Acceptance Criteria

- [ ] `GET /users/{userId}/stats` returns HTTP 200.
- [ ] Response contains `userId`, `totalAttempts` (integer), `averageScore` (two decimal places).
- [ ] `totalAttempts` reflects only submitted attempts, not open ones.
- [ ] `averageScore` is the correct average across all submitted attempt percentages.
- [ ] User with no completed attempts returns `totalAttempts: 0, averageScore: 0.00`.
- [ ] After 3 attempts with percentages 60, 80, 100, `averageScore = 80.00` and `totalAttempts = 3`.
- [ ] Open attempts (not yet submitted) are excluded from both counts.
- [ ] Malformed `userId` (not a valid UUID) returns HTTP 400.
- [ ] Swagger UI documents the 200 response.

---

## Out of Scope

- Per-quiz breakdown statistics.
- Statistics filtered by date range.
- Statistics across all users (admin aggregate view).
- Streaks, badges, or gamification metrics.
