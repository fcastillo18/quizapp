# Feature PRD: Quiz Management

## 1. Feature Name

Quiz Management — catalog browsing and quiz creation endpoints.

## 2. Epic

- Parent PRD: [`docs/planning/prd.md`](/docs/planning/prd.md)
- User stories covered: US-1, US-2, US-9

## 3. Goal

**Problem:**
Instructors have no API to create structured quizzes with questions and multiple-choice options. Students have no way to discover available quizzes or review their questions before committing to an attempt. Without a centralized, read-safe catalog, the learning workflow cannot begin. The critical constraint is that correct answers must never be exposed through any read endpoint, or the assessment value is destroyed.

**Solution:**
Three endpoints covering the full quiz lifecycle from an instructor's and student's perspective: a paginated catalog (`GET /api/quizzes`), a detail view with questions and options but no correct answers (`GET /api/quizzes/{quizId}`), and an instructor creation endpoint (`POST /api/quizzes`) with structural validation.

**Impact:**
- Students can self-direct learning by browsing available quizzes.
- Instructors can add new quiz content on demand without database access.
- Zero risk of answer leakage — enforced at the DTO layer, not the database layer.

## 4. User Personas

**Alex — AI Developer Student**
Uses the API via Postman or a thin client. Wants to browse quizzes by topic and understand the structure (number of questions, options per question) before starting. Does not need to know correct answers.

**Instructor / System Admin**
Creates quizzes programmatically or via Postman. Expects the API to reject structurally invalid quizzes (e.g., no correct answer, only one option). Trusts that data entered is persisted exactly as submitted.

## 5. User Stories

**US-1**: As a student, I want to browse a paginated list of available quizzes so I can choose one to study.

**US-2**: As a student, I want to view the full questions and options of a quiz so I can understand its scope before starting an attempt.

**US-9**: As an instructor, I want to create a quiz with questions and multiple-choice options so that students have new content to practice with.

## 6. Requirements

### Functional Requirements

**GET /api/quizzes** (US-1)
- Accepts optional query params: `page` (integer, 0-based, default `0`) and `size` (integer, default `20`, max `100`).
- Returns a paginated envelope:
  ```json
  {
    "content": [{ "id": "...", "title": "...", "description": "..." }],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  }
  ```
- `content` items include only `id`, `title`, `description`. No questions, options, or answers.
- Returns HTTP 200 with empty `content` array when no quizzes exist.
- Returns HTTP 400 with RFC 7807 error body if `size > 100`.

**GET /api/quizzes/{quizId}** (US-2)
- Returns quiz metadata (`id`, `title`, `description`) plus the full ordered list of questions.
- Each question includes: `id`, `text`, `position`, and a list of options (`id`, `text`).
- `isCorrect` and `explanation` are **never included** in the response.
- Questions are ordered by `position` ascending.
- Returns HTTP 404 if `quizId` does not exist.

**POST /api/quizzes** (US-9)
- Request body:
  ```json
  {
    "title": "string (required, max 255)",
    "description": "string (optional)",
    "questions": [
      {
        "text": "string (required)",
        "explanation": "string (required)",
        "options": [
          { "text": "string (required)", "isCorrect": true|false }
        ]
      }
    ]
  }
  ```
- Validation rules (all violations return HTTP 400):
  - `title` is required and non-blank.
  - `questions` must have ≥ 1 item.
  - Each question must have `text` (non-blank) and `explanation` (non-blank).
  - Each question must have ≥ 2 options.
  - Each question must have **exactly 1** option with `isCorrect: true`.
- Returns HTTP 201 with body `{ "id": "<uuid>" }` on success.
- `position` is auto-assigned based on question order in the request array (0-indexed or 1-indexed, consistent).

### Non-Functional Requirements

- `GET /api/quizzes` must return within 200ms for datasets up to 1,000 quizzes (local environment).
- `isCorrect` and `explanation` must not appear in any `GET` response body under any circumstance — enforced by dedicated response DTOs, not `@JsonIgnore` on entity fields.
- All string inputs are validated via Bean Validation (`@NotBlank`, `@Size`, `@NotEmpty`).
- Error responses use RFC 7807 Problem Details format: `{ type, title, status, detail }`.
- Quizzes are immutable after creation — no edit or delete endpoints exist.

## 7. Acceptance Criteria

### US-1: Paginated quiz list

**AC-1.1 — Default pagination**
- Given: 3 quizzes exist in the database
- When: `GET /api/quizzes` is called with no query params
- Then: HTTP 200; `content` contains 3 items; each item has `id`, `title`, `description`; `page=0`, `size=20`, `totalElements=3`, `totalPages=1`

**AC-1.2 — Custom page and size**
- Given: 25 quizzes exist
- When: `GET /api/quizzes?page=1&size=10`
- Then: HTTP 200; `content` contains 10 items (quizzes 11–20); `page=1`, `size=10`, `totalElements=25`, `totalPages=3`

**AC-1.3 — Empty catalog**
- When: `GET /api/quizzes` and no quizzes exist
- Then: HTTP 200; `content` is an empty array; `totalElements=0`

**AC-1.4 — Size exceeds maximum**
- When: `GET /api/quizzes?size=101`
- Then: HTTP 400; RFC 7807 error body with `detail` explaining the size limit

**AC-1.5 — No answer leakage**
- When: `GET /api/quizzes` on any dataset
- Then: Response body contains no `isCorrect` or `explanation` fields at any nesting level

### US-2: Quiz detail view

**AC-2.1 — Full detail with ordered questions**
- Given: Quiz "Agent Fundamentals" with 5 questions, each with 4 options
- When: `GET /api/quizzes/{quizId}`
- Then: HTTP 200; `questions` array has 5 items ordered by `position`; each question has `id`, `text`, `position`, and `options` array; each option has `id` and `text`

**AC-2.2 — No correct answer in response**
- When: `GET /api/quizzes/{quizId}`
- Then: Response JSON contains no `isCorrect` or `explanation` at any depth

**AC-2.3 — Not found**
- When: `GET /api/quizzes/00000000-0000-0000-0000-000000000000`
- Then: HTTP 404; RFC 7807 error body

### US-9: Quiz creation

**AC-9.1 — Successful creation**
- When: `POST /api/quizzes` with valid title, 2 questions each with 4 options (exactly 1 correct)
- Then: HTTP 201; body contains `id` (UUID); a subsequent `GET /api/quizzes/{id}` returns the quiz

**AC-9.2 — Missing title**
- When: `POST /api/quizzes` with blank `title`
- Then: HTTP 400; RFC 7807 error body naming `title` as invalid field

**AC-9.3 — No questions**
- When: `POST /api/quizzes` with `questions: []`
- Then: HTTP 400

**AC-9.4 — Question with no correct option**
- When: `POST /api/quizzes` where a question has all options `isCorrect: false`
- Then: HTTP 400; error references the invalid question

**AC-9.5 — Question with multiple correct options**
- When: `POST /api/quizzes` where a question has 2 options with `isCorrect: true`
- Then: HTTP 400

**AC-9.6 — Question with only one option**
- When: `POST /api/quizzes` where a question has 1 option
- Then: HTTP 400

**AC-9.7 — Missing explanation**
- When: `POST /api/quizzes` where a question has blank `explanation`
- Then: HTTP 400

## 8. Out of Scope

- Quiz editing or deletion — the API is append-only.
- Quiz categories or tags — deferred to v1.1.
- Bulk quiz import (e.g., CSV or JSON file upload).
- Searching or filtering quizzes by title/keyword.
- Authentication or authorization — any caller can create or read quizzes.
- Pagination for questions within a quiz detail view — all questions are always returned.
- User registration — users are pre-seeded via Liquibase.
