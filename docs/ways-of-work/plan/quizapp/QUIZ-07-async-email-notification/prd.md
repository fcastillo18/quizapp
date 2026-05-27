# QUIZ-07 — Async Email Notification Service

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** [QUIZ-01 — Database Schema & Seed Data](../QUIZ-01-database-schema-seed-data/prd.md)  
**Required by:** [QUIZ-06 — Submit Quiz Answers with Scoring](../QUIZ-06-submit-answers-scoring/prd.md)

---

## Goal

**Problem:** When a learner completes a quiz, there is no mechanism to inform them of their results outside the HTTP response. A real-world system needs asynchronous communication to decouple result delivery from the submission transaction.

**Solution:** A mocked email notification service, invoked asynchronously via Spring `@Async` after a quiz is submitted. The service simulates sending a results summary email, tracks notification status in the database, and handles failures without surfacing them to the caller.

**Impact:** Demonstrates an async communication pattern. Ensures submission reliability (zero failures caused by notification layer). Provides a clean interface contract so a real email provider can be swapped in during Phase 2.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Learner** | Receives (simulated) email with quiz results summary. |
| **Developer** | Observes async dispatch and status tracking in logs and the `notifications` table. |

---

## User Stories

- As a learner, I want to receive an email with my quiz results so that I have a record of my performance.
- As a system, I want the notification dispatch to happen asynchronously so that the submission response is not delayed by email processing.
- As a system, I want notification failures to be caught and recorded so that submission is never blocked by a failing email service.

---

## Requirements

### Functional Requirements

**Interface:**
```java
public interface EmailService {
    void sendResultsEmail(NotificationPayload payload);
}
```

- `NotificationPayload` contains: `userName`, `userEmail`, `quizTitle`, `score` (correct/total string), `percentage`, `feedbackMessage`, `completedAt` (ISO 8601).

**Mock Implementation (`MockEmailService`):**
- Implements `EmailService`.
- Annotated with `@Service` and registered as the active implementation.
- Simulates processing with `Thread.sleep(100)` (configurable via property `notification.mock.delay-ms`, default 100).
- Logs the notification payload at INFO level (excluding email address — log only name and quiz title).
- Does not throw under normal conditions.

**`NotificationService`:**
- Annotated with `@Async("notificationExecutor")`.
- Called by `AttemptService` after the submission transaction commits (invoked on the returned object, not `this`, to ensure proxy interception).
- Loads the attempt and user data, builds a `NotificationPayload`, and delegates to `EmailService`.
- Catches all `Exception`; on exception: updates `notifications.status = FAILED`, updates `notifications.updated_at`, logs error at ERROR level.
- On success: updates `notifications.status = SENT`, updates `notifications.updated_at`.

**Thread Pool Configuration:**
```java
@Bean(name = "notificationExecutor")
public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("notification-");
    return executor;
}
```

**`AsyncUncaughtExceptionHandler`:**
- Registered via `AsyncConfigurer` to catch any uncaught async exceptions and log them at ERROR level — failsafe beyond the try/catch in `NotificationService`.

**Email content (logged / captured in mock):**
- User's name and email
- Quiz title
- Score (`4/5`)
- Percentage (`80.00%`)
- Performance feedback message
- Completion timestamp (ISO 8601 UTC)

### Non-Functional Requirements

- `MockEmailService` is the only implementation. `EmailService` is an interface so a real provider can be substituted in Phase 2 with no changes to `NotificationService`.
- No PII (email address) written to logs.
- `@EnableAsync` must be present on a `@Configuration` class.
- Async dispatch is tested with `Awaitility` to avoid race conditions in integration tests.

---

## Acceptance Criteria

- [ ] After `POST /attempts/{id}/submit`, a `notifications` row exists with `status = PENDING` then transitions to `SENT` within 2 seconds (verified with Awaitility).
- [ ] `MockEmailService.sendResultsEmail` is called with correct payload fields (verified via Mockito spy in test).
- [ ] When `MockEmailService` throws, `notifications.status` transitions to `FAILED` and submission still returned HTTP 200.
- [ ] Email address is absent from all log output at INFO level.
- [ ] Thread names in logs are prefixed with `notification-`.
- [ ] `AsyncUncaughtExceptionHandler` is registered and logs at ERROR when an uncaught async exception occurs.
- [ ] The notification dispatch does not block the submission response — verified by asserting response time < 300 ms while mock delay is 100 ms (response < delay + overhead).

---

## Out of Scope

- Sending real emails (Phase 2 concern).
- Email template rendering or HTML content.
- Retry logic for failed notifications.
- Notification status query endpoint (Phase 2 extension).
