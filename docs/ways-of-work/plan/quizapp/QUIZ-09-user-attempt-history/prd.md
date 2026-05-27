# QUIZ-09 — User Attempt History

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01](../QUIZ-01-database-schema-seed-data/prd.md), [QUIZ-06](../QUIZ-06-submit-answers-scoring/prd.md)

**Phase:** 2

---

## Goal

**Problem:** Learners have no way to see a history of the quizzes they have taken. Without this, they cannot track engagement or know which quizzes they have already completed.

**Solution:** A `GET /users/{userId}/attempts` endpoint that returns a paginated-ready list of all attempts for a user, summarizing quiz title, timing, score, and percentage per attempt.

**Impact:** Enables learners to review past engagement and decide whether to retake a quiz. Also serves as the entry point into QUIZ-10 (detailed attempt results).

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Reviews their history of completed quiz attempts. |

---

## User Stories

- As a learner, I want to see all my past quiz attempts so that I can track which quizzes I have taken.
- As a learner, I want each entry to show the quiz title and my score so that I can spot quizzes I want to retake.

---

## Requirements

### Functional Requirements

- `GET /users/{userId}/attempts` returns HTTP 200 with a JSON array.
- Each element contains:
  - `attemptId` (UUID)
  - `quizId` (UUID)
  - `quizTitle` (string — joined from the quizzes table)
  - `startedAt` (ISO 8601 UTC)
  - `submittedAt` (ISO 8601 UTC, nullable — null if attempt is still open)
  - `score` (integer, nullable)
  - `percentage` (number, two decimal places, nullable)
- Results ordered by `started_at` descending (most recent first).
- Returns HTTP 200 with an empty array `[]` if the user has no attempts — never HTTP 404 for a valid-format `userId`.
- Both submitted and open (not yet submitted) attempts are returned; null values indicate open attempts.
- Implemented via `UserController` → `ProgressService` → `AttemptRepository`.
- `ProgressService` returns a `List<AttemptSummaryResponse>` DTO — no entity exposure.

### Non-Functional Requirements

- No pagination required for this scope (extension point — see Out of Scope).
- Endpoint documented in SpringDoc OpenAPI with a 200 response example.
- `quizTitle` must be retrieved via a JOIN query, not N+1 lazy loading.

---

## Acceptance Criteria

- [ ] `GET /users/{userId}/attempts` returns HTTP 200.
- [ ] Response is a JSON array; each element has `attemptId`, `quizId`, `quizTitle`, `startedAt`, `submittedAt`, `score`, `percentage`.
- [ ] `quizTitle` matches the title of the associated quiz (not null, not empty).
- [ ] Results are ordered most-recent first (`startedAt` descending).
- [ ] A user with no attempts receives `[]` with HTTP 200.
- [ ] Open attempts (not yet submitted) appear in the list with `submittedAt`, `score`, `percentage` as null.
- [ ] Retaking the same quiz creates two separate entries in the list.
- [ ] Swagger UI documents the 200 response with the full response schema.

---

## Out of Scope

- Pagination or filtering by quiz or date range.
- Deleting or archiving attempts.
- Returning per-question detail (see QUIZ-10).
