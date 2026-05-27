# QuizApp

A RESTful API for a quiz platform focused on AI development concepts. Users can browse quizzes, attempt them, receive scored feedback, and track their history. Quiz completions trigger async email notifications (mocked).

**Stack:** Spring Boot 4 · Java 21 · PostgreSQL · Liquibase · Gradle · H2 (tests)

---

## Quick Start

### Prerequisites

- Java 21+
- Docker (for PostgreSQL via Docker Compose)

### Run the app

```bash
./gradlew bootRun
```

Docker Compose starts PostgreSQL automatically. Liquibase migrations and seed data run on startup.

- API base URL: `http://localhost:8080`
- OpenAPI / Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run tests

```bash
./gradlew test          # all tests (uses H2 in-memory)
./gradlew checkstyleMain checkstyleTest  # Google Java Style enforcement
```

---

## API Endpoints

### Quiz Management

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/quizzes` | List all quizzes (id, title, description) |
| `GET` | `/quizzes/{id}` | Full quiz details with questions and options — **no correct answers exposed** |
| `POST` | `/quizzes` | Create a new quiz |

### Quiz Attempts

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/quizzes/{quizId}/attempts` | Start a new attempt for a user |
| `POST` | `/attempts/{attemptId}/submit` | Submit answers — returns score, percentage, feedback, and per-question results |
| `GET` | `/attempts/{attemptId}` | Detailed breakdown of a submitted attempt |

### User Progress

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/users/{userId}/attempts` | All quiz attempts for a user |
| `GET` | `/users/{userId}/stats` | Aggregate stats (total attempts, average score) |

---

## Scoring & Feedback

| Score | Feedback |
|-------|---------|
| ≥ 80% | "Excellent work! Keep it up!" |
| 60–79% | "Good effort! Review the topics you missed." |
| < 60% | "Keep practicing! Focus on the areas where you struggled." |

---

## Async Notifications

When a quiz is submitted, the API asynchronously calls `NotificationService.sendResultEmail(...)`. The email service is mocked (`MockEmailService`) — no real emails are sent. Notification status (`PENDING` → `SENT` / `FAILED`) is persisted. A submission failure in the email layer never fails the HTTP response.

---

## Seed Data

Two quizzes are preloaded with 5 questions each:

| Quiz | ID prefix |
|------|-----------|
| LLM Fundamentals | `bbbbbbbb-…-0001` |
| Agent Design Patterns | `bbbbbbbb-…-0002` |

Two seed users: **Alice Learner** (`aaaaaaaa-…-0001`) and **Bob Student** (`aaaaaaaa-…-0002`).

---

## Project Structure

```
src/main/java/com/fsl/quizapp/
├── quiz/          # Quiz, Question, Option entities + CRUD endpoints
├── attempt/       # Attempt, Answer entities + start/submit/detail endpoints
├── notification/  # Async email notification service (mocked)
├── progress/      # User attempt history + aggregate stats
├── user/          # User entity + history/stats controllers
└── common/        # Exceptions, GlobalExceptionHandler, PagedResponse

src/main/resources/db/changelog/
├── db.changelog-master.yaml
├── db.changelog-1.0-initial-schema.yaml
└── db.changelog-1.1-seed-data.yaml
```

---

## Testing

| Test class | Type | What it covers |
|-----------|------|----------------|
| `EndToEndQuizFlowTest` | Integration | Full 11-step API usage scenario end-to-end |
| `AttemptIntegrationTest` | Integration | Start attempt, submit, 409/404 edge cases |
| `NotificationIntegrationTest` | Integration | Async dispatch and failure non-blocking behavior |
| `QuizIntegrationTest` | Integration | List, detail, create quiz flows |
| `UserAttemptHistoryIntegrationTest` | Integration | Attempt history endpoint |
| `UserStatsIntegrationTest` | Integration | Aggregate stats endpoint |
| `AttemptDetailIntegrationTest` | Integration | Per-question detail breakdown |
| `QuizServiceTest` / `AttemptServiceTest` | Unit | Business logic, sorting, validation |
| `ScoringServiceTest` | Unit | Percentage calculation and feedback thresholds |
| `NotificationServiceTest` | Unit | Email dispatch and failure handling |
| `QuizControllerTest` / `AttemptControllerTest` | Controller slice | HTTP layer, request validation, Location headers |
