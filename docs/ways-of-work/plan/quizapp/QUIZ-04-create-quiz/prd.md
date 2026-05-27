# QUIZ-04 — Create Quiz

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01 — Database Schema & Seed Data](../QUIZ-01-database-schema-seed-data/prd.md)  
**Required by:** [QUIZ-08](../QUIZ-08-unit-integration-test-suite/prd.md)

---

## Goal

**Problem:** The system needs a way to add quizzes beyond the initial seed data. Without a creation endpoint, the quiz catalog is static and cannot grow.

**Solution:** A `POST /quizzes` endpoint that accepts a full quiz payload — title, description, and a list of questions each with options, correct answer index, and explanation — and persists it as a new quiz.

**Impact:** Enables dynamic content growth. Also used as a utility in integration tests to create controlled test data without relying solely on seed data.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Content Author** | Creates new quizzes for learners to take. |

---

## User Stories

- As a content author, I want to create a new quiz with questions and options so that learners have new content to study.
- As a content author, I want validation feedback when I submit an incomplete quiz so that I can fix the payload before it is saved.

---

## Requirements

### Functional Requirements

- `POST /quizzes` accepts a JSON body.
- Request body structure:
  ```json
  {
    "title": "string (required, non-blank)",
    "description": "string (optional)",
    "questions": [
      {
        "text": "string (required, non-blank)",
        "explanation": "string (required, non-blank)",
        "position": "int (required, >= 1)",
        "options": [
          { "text": "string (required, non-blank)", "position": "int (required, >= 1)" }
        ],
        "correctOptionPosition": "int (required — 1-based index into the options array)"
      }
    ]
  }
  ```
- Validation rules:
  - `title` must be non-blank.
  - `questions` must not be null or empty.
  - Each question must have at least 2 options.
  - `correctOptionPosition` must be a valid index (1 ≤ value ≤ options.size).
- On success, persists quiz, questions, and options atomically (single `@Transactional` boundary). Sets `correct_option_id` on each question after options are inserted.
- Returns HTTP 201 with body `{ "id": "<UUID>" }` and a `Location: /quizzes/{id}` header.
- Returns HTTP 400 with a validation error body for any constraint violation.
- `QuizController` delegates all persistence to `QuizService`. No SQL in the controller.

### Non-Functional Requirements

- The entire quiz creation is wrapped in a single transaction — partial saves must not occur.
- Endpoint documented in SpringDoc OpenAPI with 201 and 400 response examples.
- HTTP 201 response includes a `Location` header pointing to the created resource (`/quizzes/{id}`).
- All timestamps in responses use ISO 8601 UTC format (e.g., `2026-05-27T14:00:00Z`).
- Error responses use the standard shape: `{ "status": <code>, "message": "<human-readable>", "timestamp": "<ISO>" }` (applicable to HTTP 400 cases).

---

## Acceptance Criteria

- [ ] `POST /quizzes` with a valid payload returns HTTP 201.
- [ ] Response body is `{ "id": "<UUID>" }`.
- [ ] `Location` header is present and points to `/quizzes/{id}`.
- [ ] Created quiz is retrievable via `GET /quizzes/{id}` immediately after creation.
- [ ] Missing `title` returns HTTP 400.
- [ ] Empty `questions` array returns HTTP 400.
- [ ] `correctOptionPosition` out of range returns HTTP 400.
- [ ] No partial quiz is saved on validation failure (transaction rolled back).
- [ ] Swagger UI documents 201 and 400 responses.

---

## Out of Scope

- Quiz editing (PUT/PATCH) or deletion (DELETE).
- Assigning quizzes to categories or tags.
- Image or media attachments on questions.
- Bulk quiz import.
