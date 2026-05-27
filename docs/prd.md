# PRD — QuizApp REST API

**Version**: 1.0  
**Author**: Franklin Castillo  
**Date**: 2026-05-27  
**Status**: Draft

---

## 1. Executive Summary

**Problem Statement**  
There is no structured backend system for delivering, tracking, and providing feedback on quizzes covering AI development concepts. Learning progress is not persisted, results are not communicated asynchronously, and there is no aggregate visibility into a learner's performance over time.

**Proposed Solution**  
A RESTful API built on Spring Boot 4 that manages quiz content, records quiz attempts, scores submissions with contextual feedback, and asynchronously notifies users of their results via a mocked email service. Progress history and aggregate statistics are fully queryable.

**Success Criteria**

| KPI | Target |
|---|---|
| All specified endpoints implemented and returning correct HTTP status codes | 100% |
| Quiz submission response time (scoring + feedback) | < 500 ms p99 |
| Async email trigger: submission must not block or fail due to notification errors | 0 submission failures caused by notification layer |
| At least 2 quizzes with 5+ questions each seeded at startup | Verified via integration test |
| Unit test coverage for scoring logic, feedback calculation, and async workflow | >= 80% line coverage on service layer |

---

## 2. User Experience & Functionality

### 2.1 User Personas

| Persona | Description |
|---|---|
| **Learner** | A developer studying AI concepts. Identified by a `userId` passed in each request. No authentication layer. |
| **Content Author** | Creates quizzes via the quiz creation endpoint. Same identity model as Learner for this scope. |

### 2.2 User Stories & Acceptance Criteria

---

**Story 1 — Browse available quizzes**  
*As a learner, I want to list all quizzes so that I can choose one to take.*

- `GET /quizzes` returns an array of objects containing `id`, `title`, and `description` only.
- Correct answers and question details are NOT included.
- Returns HTTP 200 with an empty array if no quizzes exist.

---

**Story 2 — View quiz details before starting**  
*As a learner, I want to see all questions and options for a quiz so that I know what to expect.*

- `GET /quizzes/{quizId}` returns full quiz with all questions and their multiple-choice options.
- Correct answers are NOT present in the response.
- Returns HTTP 404 if `quizId` does not exist.

---

**Story 3 — Create a quiz**  
*As a content author, I want to create a new quiz with questions so that learners can take it.*

- `POST /quizzes` accepts title, description, and a list of questions (each with options, correct answer index, and explanation).
- Returns HTTP 201 with the created quiz `id`.
- Returns HTTP 400 if required fields are missing or the question list is empty.

---

**Story 4 — Start a quiz attempt**  
*As a learner, I want to start a new attempt so that I can answer the questions.*

- `POST /quizzes/{quizId}/attempts` accepts `userId` in the request body.
- Returns HTTP 201 with the attempt `id`, `quizId`, `userId`, `startedAt` timestamp, and the full question+options list (no correct answers).
- A user can start multiple attempts for the same quiz; each is a distinct record.
- Returns HTTP 404 if `quizId` does not exist.

---

**Story 5 — Submit answers and receive scored results**  
*As a learner, I want to submit my answers and immediately receive my score and per-question feedback.*

- `POST /attempts/{attemptId}/submit` accepts `userId` and a list of `{ questionId, selectedOptionId }` pairs.
- Scores the submission, persists the result, and returns HTTP 200 with:
  - `score` (correct count / total questions)
  - `percentage` (rounded to two decimal places)
  - `feedbackMessage` — contextual string:
    - `>= 80%` → encouraging message (e.g., "Great work! Keep it up!")
    - `60–79%` → motivational message (e.g., "Good effort! Review the missed topics.")
    - `< 60%` → improvement-focused message (e.g., "Keep practicing — you'll get there!")
  - Per-question breakdown: `questionId`, `correct` (boolean), `explanation`
- Asynchronously triggers email notification after scoring completes; notification failure must NOT affect the HTTP response or persistence.
- Returns HTTP 404 if `attemptId` is not found.
- Returns HTTP 409 if the attempt has already been submitted.

---

**Story 6 — View attempt history**  
*As a learner, I want to see all my past quiz attempts so that I can track my engagement.*

- `GET /users/{userId}/attempts` returns a list of all attempts for that user: `attemptId`, `quizId`, `quizTitle`, `startedAt`, `submittedAt`, `score`, `percentage`.
- Returns HTTP 200 with an empty array if the user has no attempts.

---

**Story 7 — View detailed attempt results**  
*As a learner, I want to review a specific attempt in detail so that I can learn from my mistakes.*

- `GET /attempts/{attemptId}` returns:
  - `attemptId`, `userId`, `quizId`, `quizTitle`
  - `startedAt`, `submittedAt`
  - `overallScore`, `percentage`
  - Per-question breakdown: `questionId`, `question text`, `selectedOption`, `correctOption`, `correct` (boolean), `explanation`
- Returns HTTP 404 if `attemptId` is not found or does not belong to the requesting `userId`.

---

**Story 8 — View aggregate statistics**  
*As a learner, I want to see my overall quiz statistics so that I can understand my progress.*

- `GET /users/{userId}/stats` returns:
  - `totalAttempts` (integer)
  - `averageScore` (percentage, two decimal places)
- Returns HTTP 200 with zeroed values if the user has no completed attempts.

---

### 2.3 Non-Goals

The following are explicitly out of scope for this delivery:

- User registration, login, or JWT/session authentication.
- Sending real emails (email service is mocked).
- Quiz editing or deletion endpoints.
- Question-level partial updates.
- Pagination or filtering on list endpoints.
- Rate limiting or API key management.
- Admin dashboard or frontend UI.

---

## 3. Technical Specifications

### 3.1 Architecture Overview

```
Client
  │
  ▼
Spring Boot REST Controllers (HTTP layer)
  │
  ├── QuizController       → QuizService
  ├── AttemptController    → AttemptService ─── ScoringService
  │                                         └── NotificationService (async)
  └── UserController       → ProgressService

Service Layer
  │
  ├── Spring Data JPA Repositories
  │     └── PostgreSQL (prod/local) | H2 (test profile)
  │
  └── NotificationService
        └── EmailNotificationService (mocked, @Async)
```

**Key design constraints:**

- Controllers own only request/response mapping. No business logic in controllers.
- `ScoringService` is pure (no I/O) — scores a submission given questions and answers. Unit-testable without Spring context.
- `NotificationService` dispatches async via Spring `@Async` + `@EnableAsync`. Failures are caught, logged, and recorded in a `notifications` table; they never propagate to the caller.
- Liquibase manages all schema changes. Hibernate DDL auto is set to `validate` in all profiles.

### 3.2 Data Model

| Table | Key Columns |
|---|---|
| `quizzes` | `id`, `title`, `description`, `created_at` |
| `questions` | `id`, `quiz_id`, `text`, `correct_option_id`, `explanation`, `position` |
| `options` | `id`, `question_id`, `text`, `position` |
| `users` | `id`, `name`, `email` |
| `attempts` | `id`, `user_id`, `quiz_id`, `started_at`, `submitted_at`, `score`, `percentage` |
| `answers` | `id`, `attempt_id`, `question_id`, `selected_option_id`, `correct` |
| `notifications` | `id`, `attempt_id`, `status` (`PENDING`/`SENT`/`FAILED`), `created_at`, `updated_at` |

All schema changes delivered as Liquibase changesets under `src/main/resources/db/changelog/`.

### 3.3 Integration Points

| Concern | Detail |
|---|---|
| **Database** | PostgreSQL 16 via Docker Compose (local/prod). H2 in-memory for `test` profile. |
| **Async execution** | Spring `@Async` with a dedicated `ThreadPoolTaskExecutor` bean. |
| **Email service** | `MockEmailService` implementing an `EmailService` interface. Logs notification details; simulates 100 ms latency with `Thread.sleep`. |
| **API docs** | SpringDoc OpenAPI — Swagger UI at `/swagger-ui.html`. |
| **Migrations** | Liquibase master changelog at `src/main/resources/db/changelog/db.changelog-master.yaml`. |

### 3.4 API Response Conventions

- All timestamps: ISO 8601 UTC (`2026-05-27T14:00:00Z`).
- Error responses: `{ "status": <code>, "message": "<human-readable>", "timestamp": "<ISO>" }`.
- HTTP 201 responses include a `Location` header pointing to the created resource.

### 3.5 Security & Privacy

- No authentication in scope; `userId` is a trusted caller-supplied value.
- Correct answers are never returned in quiz detail or attempt-start responses.
- User email addresses are stored but only transmitted to the (mocked) notification service.
- No PII is logged at INFO level or above.

---

## 4. Seed Data Requirements

At application startup (via Liquibase data changeset), the database must contain:

- **2 quizzes**, each with **5+ questions**, on AI development concepts (e.g., "LLM Fundamentals", "Agent Design Patterns").
- At least **2 users** with name and email populated, available for integration test scenarios.

---

## 5. Testing Strategy

### Unit Tests (no Spring context, `@ExtendWith(MockitoExtension.class)`)

| Target | Scenarios |
|---|---|
| `ScoringService` | All correct → 100%; all wrong → 0%; partial → correct percentage; boundary at 60% and 80% thresholds. |
| Feedback calculation | Score exactly at each boundary (60%, 80%); score above 80%; score below 60%. |
| `NotificationService` | Async dispatch called after submission; service exception does NOT propagate; notification status updated to FAILED on exception. |

### Integration Tests (`@SpringBootTest`, `@ActiveProfiles("test")`)

| Scenario | Verified via |
|---|---|
| Full happy path (browse → start → submit → results) | `MockMvc` end-to-end |
| Resubmitting an already-submitted attempt | HTTP 409 |
| Submitting for a non-existent attempt | HTTP 404 |
| Aggregate stats correct after multiple attempts | `GET /users/{id}/stats` |
| Email mock invoked asynchronously on submit | Mockito verify + `Awaitility` |

---

## 6. Risks & Roadmap

### Phase 1 — MVP (current scope)

- Quiz CRUD (list, detail, create)
- Attempt start and submission with scoring + feedback
- Async email notification (mocked)
- Notification failure isolation
- Seed data (2 quizzes × 5+ questions)
- Unit + integration test suite

### Phase 2 — Extensions

- Progress & statistics endpoints (`GET /users/{id}/attempts`, `GET /attempts/{id}`, `GET /users/{id}/stats`)
- Notification status tracking endpoint (`GET /notifications/{attemptId}`)
- Replace `MockEmailService` with a real provider (SendGrid / AWS SES) behind the same `EmailService` interface

### Technical Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `@Async` exceptions silently swallowed | Medium | Implement `AsyncUncaughtExceptionHandler`; write a test that verifies FAILED status written to DB on exception. |
| H2 / PostgreSQL dialect divergence in tests | Low | Pin Liquibase changesets to standard SQL; avoid PostgreSQL-specific types in test profile. |
| Correct answers leaked in API response | Medium | Add a response DTO projection that excludes `correctOptionId`; test explicitly with `assertThat(response).doesNotContain("correctOptionId")`. |
| Seed data conflicts on restart | Low | Use Liquibase `onFail: MARK_RAN` or unique constraint guards in data changesets. |
