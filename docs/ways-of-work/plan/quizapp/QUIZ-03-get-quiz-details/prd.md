# QUIZ-03 — Get Quiz Details

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01 — Database Schema & Seed Data](../QUIZ-01-database-schema-seed-data/prd.md)  
**Required by:** [QUIZ-08](../QUIZ-08-unit-integration-test-suite/prd.md)

---

## Goal

**Problem:** Learners need to review the full question and option text for a quiz before — or during — an attempt, but must not see which answers are correct.

**Solution:** A `GET /quizzes/{quizId}` endpoint that returns the quiz's full structure: all questions and all answer options, with correct answers explicitly withheld.

**Impact:** Enables learners to read and prepare answers for all questions before submitting. Also useful for displaying questions during an active attempt.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Reviews questions and options to prepare answers. |

---

## User Stories

- As a learner, I want to retrieve all questions and options for a specific quiz so that I know what I need to answer.
- As a learner, I want the response to hide which answer is correct so that the quiz is meaningful.

---

## Requirements

### Functional Requirements

- `GET /quizzes/{quizId}` returns HTTP 200 with the full quiz object.
- Response includes: `id`, `title`, `description`, and a `questions` array.
- Each question in the array includes: `id`, `text`, `position`, and an `options` array.
- Each option includes: `id`, `text`, `position`.
- `correctOptionId` is never present anywhere in the response.
- `explanation` is never present in this response (shown only after submission).
- Returns HTTP 404 with a standard error body if `quizId` does not exist.
- `QuizService` maps the entity graph to a `QuizDetailResponse` DTO; the DTO class does not have a `correctOptionId` field.
- Questions are ordered by `position` ascending; options are ordered by `position` ascending.

### Non-Functional Requirements

- Correct answer must not leak through any serialization path (e.g., Jackson `@JsonIgnore` or projection DTO — not runtime filtering).
- Security: an integration test must assert `assertThat(responseBody).doesNotContain("correctOptionId")` to ensure correct answers are never returned.
- Endpoint documented in SpringDoc OpenAPI with 200 and 404 response examples.
- All timestamps in responses use ISO 8601 UTC format (e.g., `2026-05-27T14:00:00Z`).
- Error responses use the standard shape: `{ "status": <code>, "message": "<human-readable>", "timestamp": "<ISO>" }` (applicable to the HTTP 404 case).

---

## Acceptance Criteria

- [ ] `GET /quizzes/{validId}` returns HTTP 200.
- [ ] Response contains `id`, `title`, `description`, and `questions` array.
- [ ] Each question object contains `id`, `text`, `position`, and `options` array.
- [ ] Each option contains `id`, `text`, `position` and nothing else.
- [ ] `correctOptionId` is absent from the entire response body.
- [ ] `explanation` is absent from the response body.
- [ ] Questions are ordered by `position` ascending.
- [ ] Options are ordered by `position` ascending.
- [ ] `GET /quizzes/nonexistent-id` returns HTTP 404 with `{ "status": 404, "message": "...", "timestamp": "..." }`.
- [ ] Swagger UI documents both 200 and 404 responses.

---

## Out of Scope

- Returning correct answers or explanations (those are part of QUIZ-06 submission response).
- Editing or deleting a quiz.
- Filtering questions by any criteria.
