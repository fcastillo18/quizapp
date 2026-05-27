# QUIZ-02 — List Available Quizzes

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01 — Database Schema & Seed Data](../QUIZ-01-database-schema-seed-data/prd.md)  
**Required by:** [QUIZ-08](../QUIZ-08-unit-integration-test-suite/prd.md)

---

## Goal

**Problem:** Learners have no way to discover which quizzes are available without querying the database directly.

**Solution:** A `GET /quizzes` endpoint that returns a lightweight summary list — id, title, and description — so learners can browse and choose.

**Impact:** Entry point of the quiz-taking flow. Enables learners to select a quiz before starting an attempt.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Browses the catalog to pick a quiz to take. |

---

## User Stories

- As a learner, I want to retrieve all available quizzes so that I can see what topics I can study.
- As a learner, I want the list to omit questions and correct answers so that I am not spoiled before starting.

---

## Requirements

### Functional Requirements

- `GET /quizzes` returns HTTP 200 with a JSON array.
- Each element contains exactly: `id` (UUID), `title` (string), `description` (string).
- Questions, options, and correct answers are never included in this response.
- Returns an empty array `[]` when no quizzes exist — never HTTP 404.
- Response is served by `QuizController` → `QuizService` → `QuizRepository`.
- `QuizService` maps `Quiz` entities to a `QuizSummaryResponse` DTO — no entity exposure on the controller boundary.

### Non-Functional Requirements

- Response must not serialize internal fields (`createdAt`, `correct_option_id`, etc.).
- No pagination required for this scope.
- Endpoint documented in SpringDoc OpenAPI with a 200 response example.
- All timestamps in responses use ISO 8601 UTC format (e.g., `2026-05-27T14:00:00Z`).
- Error responses (if introduced in future) must use the standard shape: `{ "status": <code>, "message": "<human-readable>", "timestamp": "<ISO>" }`.

---

## Acceptance Criteria

- [ ] `GET /quizzes` returns HTTP 200.
- [ ] Response body is a JSON array; each element has `id`, `title`, `description` and no other fields.
- [ ] Correct answers (`correctOptionId`) are absent from the response — verified with `assertThat(body).doesNotContain("correctOptionId")`.
- [ ] When the quizzes table is empty, response is `[]` with HTTP 200 (not 404).
- [ ] Seed data (2 quizzes from QUIZ-01) appears in the list when running the `test` profile.
- [ ] Swagger UI shows the endpoint with a documented 200 response.

---

## Out of Scope

- Filtering or searching quizzes by title or category.
- Pagination or sorting.
- Quiz questions or options in this response.
