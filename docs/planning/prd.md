# PRD: Quiz App REST API

**Version**: 1.0
**Date**: 2026-05-25
**Stack**: Spring Boot 4.0.6 · Java 21 · PostgreSQL · Liquibase · Gradle

---

## 1. Executive Summary

**Problem Statement**
Developers learning AI development concepts lack a structured, self-assessable tool that provides immediate scoring feedback and tracks progress over time through a simple API.

**Proposed Solution**
A RESTful backend API that manages quizzes, records user attempts, scores submissions with contextual feedback, and asynchronously notifies users of results via a mocked email service—all without requiring authentication.

**Success Criteria**

| KPI | Target |
|-----|--------|
| All required endpoints implemented and returning correct HTTP status codes | 100% |
| Quiz submission returns score + per-question feedback within 500ms (local) | ≥ 99% of requests |
| Async email notification fires after every completed attempt | 100% trigger rate |
| Notification failure does not affect HTTP 200 on quiz submission | Always |
| At least 2 preloaded quizzes with ≥ 5 questions each | 2 quizzes, 10+ questions |
| Unit test coverage for scoring, feedback, and async workflow | ≥ 80% line coverage on service layer |

---

## 2. User Experience & Functionality

### 2.1 User Personas

**Alex — AI Developer Student**
A software engineer completing a training program on AI development concepts. Alex uses the API directly (via a client like Postman or a thin frontend). Wants immediate, clear feedback to identify knowledge gaps.

**Instructor / System Admin**
Creates and seeds quizzes via the API. Cares about data integrity: questions must not expose correct answers to students, and attempt history must be immutable.

---

### 2.2 User Stories & Acceptance Criteria

#### Quiz Discovery

**US-1**: As a student, I want to list all available quizzes so I can choose what to study.
- `GET /api/quizzes` accepts optional query params `page` (0-based, default `0`) and `size` (default `20`, max `100`).
- Returns a paginated envelope: `{ content: [...], page, size, totalElements, totalPages }`. Each item has `id`, `title`, and `description` only.
- Correct answers and explanations are never included.
- Returns HTTP 200 with an empty `content` array when no quizzes exist.
- Returns HTTP 400 if `size` exceeds 100.

**US-2**: As a student, I want to see the full details of a quiz (questions + options) before starting.
- `GET /api/quizzes/{quizId}` returns quiz metadata plus all questions.
- Each question includes its options (text + option ID) but `isCorrect` is **omitted**.
- Returns HTTP 404 if `quizId` does not exist.

#### Quiz Attempt

**US-3**: As a student, I want to start a quiz attempt so I can record my session.
- `POST /api/quizzes/{quizId}/attempts` body: `{ "userId": "<uuid>" }`.
- Response: attempt ID, quiz details (questions + options, no answers), `startedAt` timestamp.
- Multiple attempts for the same user/quiz are allowed; each produces a new `attemptId`.
- Returns HTTP 404 if quiz or user does not exist.

**US-4**: As a student, I want to submit all my answers at once and receive immediate results.
- `POST /api/attempts/{attemptId}/submit` body: list of `{ questionId, selectedOptionId }`.
- Response includes:
  - `score` (e.g., `4/5`), `percentage` (e.g., `80.0`), `feedbackMessage`
  - Per-question breakdown: `questionId`, `isCorrect`, `explanation`
- Submission is idempotent for the same attempt (re-submission returns HTTP 409 Conflict).
- Returns HTTP 404 if `attemptId` does not exist.

**US-5**: As a student, I want contextual performance feedback based on my score.

| Score Range | Feedback Message |
|-------------|-----------------|
| ≥ 80% | "Great job! You're getting there!" |
| 60–79% | "Good effort! Keep practicing to improve your score." |
| < 60% | "Keep going! Review the material and try again." |

#### User Progress

**US-6**: As a student, I want to view all my past quiz attempts.
- `GET /api/users/{userId}/attempts` accepts optional query params `page` (0-based, default `0`) and `size` (default `20`, max `100`). Results are sorted by `completedAt` descending.
- Returns a paginated envelope: `{ content: [...], page, size, totalElements, totalPages }`. Each item has `attemptId`, `quizId`, `quizTitle`, `score`, `percentage`, `completedAt`.
- Returns HTTP 404 if `userId` does not exist.
- Returns HTTP 400 if `size` exceeds 100.

**US-7**: As a student, I want to view the full breakdown of a specific attempt.
- `GET /api/attempts/{attemptId}` returns: `attemptId`, `userId`, `quizId`, `quizTitle`, `startedAt`, `completedAt`, `score`, `percentage`, per-question breakdown with `isCorrect` and `explanation`.
- Returns HTTP 404 if attempt does not exist.

**US-8**: As a student, I want to see my aggregate statistics.
- `GET /api/users/{userId}/stats` returns: `totalAttempts`, `averageScore` (percentage, 2 decimal places).

#### Quiz Creation (Admin)

**US-9**: As an instructor, I want to create a new quiz with questions and options via the API.
- `POST /api/quizzes` body: quiz title, description, and a list of questions—each with text, explanation, and options (text + `isCorrect`).
- Returns HTTP 201 with the created quiz ID.
- Validation: quiz must have ≥ 1 question; each question must have exactly 1 correct option and ≥ 2 options total. Returns HTTP 400 otherwise.

---

### 2.3 Non-Goals

- **No authentication or authorization**: users are identified by a UUID passed in request bodies/paths. No login, no JWT, no session management.
- **No real email delivery**: the email service is mocked (logs only). No SMTP integration.
- **No quiz editing or deletion**: the API is append-only for quizzes and attempts.
- **No frontend**: this is a backend API only.
- **No timer/time-limit enforcement**: attempts have no deadline.

---

## 3. Technical Specifications

### 3.1 Data Model

```
users
  id           UUID PK
  name         VARCHAR(255) NOT NULL
  email        VARCHAR(255) NOT NULL UNIQUE

quizzes
  id           UUID PK
  title        VARCHAR(255) NOT NULL
  description  TEXT

questions
  id           UUID PK
  quiz_id      UUID FK → quizzes.id
  text         TEXT NOT NULL
  explanation  TEXT NOT NULL
  position     INT NOT NULL   -- display order

options
  id           UUID PK
  question_id  UUID FK → questions.id
  text         TEXT NOT NULL
  is_correct   BOOLEAN NOT NULL DEFAULT FALSE

quiz_attempts
  id              UUID PK
  user_id         UUID FK → users.id
  quiz_id         UUID FK → quizzes.id
  started_at      TIMESTAMP NOT NULL
  completed_at    TIMESTAMP             -- NULL until submitted
  score           INT                   -- correct answer count
  total_questions INT
  percentage      DECIMAL(5,2)

attempt_answers
  id                 UUID PK
  attempt_id         UUID FK → quiz_attempts.id
  question_id        UUID FK → questions.id
  selected_option_id UUID FK → options.id
  is_correct         BOOLEAN NOT NULL

notification_logs
  id           UUID PK
  attempt_id   UUID FK → quiz_attempts.id
  status       VARCHAR(20) NOT NULL   -- PENDING | SENT | FAILED
  created_at   TIMESTAMP NOT NULL
  sent_at      TIMESTAMP
  error_msg    TEXT
```

All schema changes are managed by Liquibase. Hibernate DDL auto is set to `validate`.

---

### 3.2 API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/quizzes?page=0&size=20` | Paginated list of quizzes (id, title, description) |
| `GET` | `/api/quizzes/{quizId}` | Full quiz with questions + options (no correct answers) |
| `POST` | `/api/quizzes` | Create a new quiz |
| `POST` | `/api/quizzes/{quizId}/attempts` | Start a quiz attempt |
| `POST` | `/api/attempts/{attemptId}/submit` | Submit answers and get scored results |
| `GET` | `/api/users/{userId}/attempts?page=0&size=20` | Paginated attempts for a user (sorted by completedAt desc) |
| `GET` | `/api/attempts/{attemptId}` | Detailed results for one attempt |
| `GET` | `/api/users/{userId}/stats` | Aggregate stats for a user |

All responses follow `application/json`. Errors use RFC 7807 Problem Details format (`type`, `title`, `status`, `detail`).

---

### 3.3 Async Email Notification

**Trigger**: Immediately after `QuizAttempt` is marked complete and score is persisted.

**Flow**:
1. `QuizSubmissionService` persists attempt and answers.
2. Calls `NotificationService.sendResultsEmail(attempt)` annotated with `@Async`.
3. Spring executes `sendResultsEmail` on a separate thread pool.
4. `MockEmailService` logs the email payload (user name, email, quiz title, score, percentage, feedback, timestamp) at `INFO` level.
5. `NotificationLog` row is updated: `PENDING → SENT` (or `FAILED` with `error_msg`).
6. HTTP response to the client is returned **before** the async task completes.

**Failure isolation**: If `sendResultsEmail` throws, the exception is caught inside the async method. The submission HTTP response is always HTTP 200.

**Thread pool config** (`application.properties`):
```
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100
```

---

### 3.4 Architecture Overview

```
Client (Postman / cURL)
        │
        ▼
  REST Controllers (@RestController)
        │
        ▼
  Service Layer (@Service)
  ├── QuizService          — quiz CRUD + question/option assembly
  ├── AttemptService       — start attempt, idempotency guard
  ├── SubmissionService    — scoring, feedback, persists answers
  └── NotificationService  — @Async, writes NotificationLog
        │
        ▼
  Repository Layer (Spring Data JPA)
        │
        ▼
  PostgreSQL (via Docker Compose in local/dev; H2 in test profile)
```

Liquibase runs at startup to apply any pending migrations.

---

### 3.5 Preloaded Data

Two quizzes must be seeded via Liquibase `loadData` or `insert` changesets:

| Quiz | Questions |
|------|-----------|
| **Agent Fundamentals** | 5 questions covering agent loops, tool use, memory, planning |
| **Prompt Engineering Basics** | 5 questions covering chain-of-thought, few-shot, system prompts, temperature |

Each question has 4 options with exactly 1 correct answer and a non-trivial explanation.

---

### 3.6 Security & Privacy

- No PII beyond user `name` and `email`, stored in `users` table.
- Correct answers are **never** returned by `GET /api/quizzes/{id}` or `POST /api/attempts/{id}/submit` — only included in the scoring logic server-side.
- No auth headers required; API is treated as internal/trusted (suitable for a training environment).
- Input validation via Bean Validation (`@NotNull`, `@NotBlank`, `@Size`) on all request DTOs.

---

## 4. Testing Requirements

### Unit Tests (no Spring context — `@ExtendWith(MockitoExtension.class)`)

| Class | Test Coverage |
|-------|--------------|
| `ScoringService` | Correct count calculation; 0/N, N/N, partial scores |
| `FeedbackService` | All three feedback bands (≥80, 60-79, <60); exact boundaries (60%, 80%) |
| `NotificationService` | Async invocation; exception does not propagate; `NotificationLog` status transitions |

### Integration Tests (`@SpringBootTest @ActiveProfiles("test")`)

| Scenario | Verified |
|----------|----------|
| Full attempt flow (start → submit → results) | Score, feedback, per-question breakdown |
| Duplicate submission returns HTTP 409 | Idempotency guard |
| Submit with unknown `attemptId` returns HTTP 404 | Error handling |
| Notification log row created after submission | Async side effect persisted |

### Controller Tests (`@WebMvcTest`)

- Correct answers absent from `GET /api/quizzes/{id}` response.
- `POST /api/quizzes` with missing required field returns HTTP 400.

---

## 5. Risks & Roadmap

### 5.1 Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Async task runs after test assertion (flaky tests) | Medium | Use `@Async` with `TaskExecutor` bean that can be replaced with `SyncTaskExecutor` in tests |
| H2 / PostgreSQL dialect mismatch in tests | Low | Restrict JPQL to standard syntax; avoid native queries |
| Correct answers accidentally serialized in response | Medium | Use dedicated response DTOs (not entity classes); Jackson `@JsonIgnore` on `isCorrect` field of `Option` entity |
| Preloaded data conflicts on repeated startup | Low | Liquibase `runOnChange: false` (default); seed changelogs are append-only |

### 5.2 Phased Rollout

**MVP (current scope)**
- All 8 endpoints implemented
- @Async mock email
- 2 seeded quizzes
- Unit + integration tests

**v1.1 (future)**
- Quiz categories / tags
- User registration endpoint (currently users must be pre-seeded or created via a separate endpoint)

**v2.0 (future)**
- Real email delivery (e.g., Spring Mail + SMTP or SendGrid)
- JWT authentication
- Leaderboard endpoint
- Time-limited attempts
