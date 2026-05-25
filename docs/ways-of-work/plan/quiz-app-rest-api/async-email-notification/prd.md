# Feature PRD: Async Email Notification

## 1. Feature Name

Async Email Notification — fire-and-forget results summary triggered after quiz submission.

## 2. Epic

- Parent PRD: [`docs/planning/prd.md`](/docs/planning/prd.md)
- Triggered by: Quiz Attempt & Submission feature (US-4 submit endpoint)
- Related section: PRD §3.3 Async Email Notification

## 3. Goal

**Problem:**
After completing a quiz, students have no automated record of their results delivered to them. The submission response is ephemeral — if the student closes the app, the results are lost unless they query the attempt detail endpoint. Additionally, the system must demonstrate asynchronous communication patterns between the core API and an external notification service.

**Solution:**
An asynchronous, fire-and-forget notification workflow triggered immediately after a successful quiz submission. A `MockEmailService` logs a structured email payload instead of delivering a real email. A `NotificationLog` table tracks status (`PENDING → SENT | FAILED`) so notification outcomes are auditable. The workflow is fully decoupled from the HTTP response — submission always returns HTTP 200 regardless of notification outcome.

**Impact:**
- Students receive (simulated) results summaries without polling the API.
- Notification failures are observable and do not degrade the core quiz experience.
- The architecture demonstrates async patterns using Spring `@Async` with a dedicated thread pool.

## 4. User Personas

**Alex — AI Developer Student**
Expects to receive a results email after submitting a quiz. Does not care whether it's a real email or a mock — the contract is that it fires. If the notification fails, Alex's submission is not affected.

**System / Developer**
Needs to verify that notifications were sent (or why they failed) without querying log files. The `NotificationLog` table serves as the audit trail.

## 5. User Stories

**US-N1**: As a student, I want to receive an email summary of my quiz results after submission so I have a record of my performance without needing to query the API.

**US-N2**: As a developer, I want notification failures to be silently swallowed so that a broken email flow never causes quiz submissions to fail.

**US-N3**: As a developer, I want notification status to be persisted so I can audit whether notifications were sent or failed and why.

## 6. Requirements

### Functional Requirements

**Trigger** (US-N1)
- Immediately after `SubmissionService` commits the quiz attempt transaction, it calls `NotificationService.sendResultsEmail(attempt)`.
- This call is made **outside** the database transaction (after commit) to avoid holding the connection during the async operation.
- The HTTP response is dispatched to the client before the async method completes.

**NotificationLog creation** (US-N3)
- Before the async method begins processing, a `NotificationLog` row is inserted with `status = PENDING` and `createdAt = now()`.
- On successful logging by `MockEmailService`: status updated to `SENT`, `sentAt = now()`.
- On any exception during processing: status updated to `FAILED`, `errorMsg` populated with the exception message.

**MockEmailService behavior** (US-N1)
- Logs the following payload at `INFO` level (no real email sent):
  - User's `name` and `email`
  - Quiz `title`
  - Score (e.g., `"4/5"`)
  - Percentage (e.g., `80.00`)
  - Performance feedback message (same string as returned in submit response)
  - `completedAt` timestamp (ISO 8601)
- Log format: structured, readable — e.g., `"[EMAIL MOCK] To: {email} | Quiz: {title} | Score: {score} | Feedback: {feedback}"`

**Failure isolation** (US-N2)
- If `MockEmailService` throws any exception, it is caught within the `@Async` method body.
- The exception must not propagate to the HTTP request thread.
- The `NotificationLog` row status is updated to `FAILED` with the error message.
- No retry logic — failures are recorded and left for manual inspection.

**Thread pool configuration**
- Spring `@Async` uses a named `TaskExecutor` bean (`notificationExecutor`) configured with:
  - `corePoolSize = 2`
  - `maxPoolSize = 5`
  - `queueCapacity = 100`
  - `threadNamePrefix = "notification-"`
- In the `test` Spring profile, this bean is replaced with `SyncTaskExecutor` so async behavior is deterministic in tests.

### Non-Functional Requirements

- The async task must not block the HTTP response thread. Submission endpoint response time must not be affected by email processing time.
- `NotificationLog` row must exist (at `PENDING` or terminal status) within 5 seconds of the HTTP response for a successful submission (local environment).
- The `notification_logs` table must never be queried or mutated from the HTTP request thread — only from the async thread.
- One `NotificationLog` row is created per submission. No duplicates.
- If the `notificationExecutor` queue is full (all 100 slots occupied), the excess task is dropped; no exception is surfaced to the caller. The `NotificationLog` row in this case may remain `PENDING` indefinitely — this is an acceptable degradation.

## 7. Acceptance Criteria

### US-N1: Notification fires after submission

**AC-N1.1 — Notification triggered on successful submission**
- Given: A valid, unseen attempt with a valid user (has `name` and `email`)
- When: `POST /api/attempts/{attemptId}/submit` with all answers
- Then: Within 5 seconds, a `NotificationLog` row exists for this `attemptId` with `status = SENT`; the mock email payload is present in application logs

**AC-N1.2 — Email log payload is complete**
- When: Notification is processed
- Then: Application log contains user `email`, user `name`, quiz `title`, score string (e.g., `"4/5"`), percentage, feedback message, and `completedAt` timestamp

**AC-N1.3 — HTTP response not delayed by notification**
- When: `POST /api/attempts/{attemptId}/submit`
- Then: HTTP response is returned before the async task completes (observable via test with `SyncTaskExecutor` disabled and a deliberate delay injected into mock)

### US-N2: Failure isolation

**AC-N2.1 — Submission succeeds even when notification throws**
- Given: `MockEmailService` is configured to throw a `RuntimeException`
- When: `POST /api/attempts/{attemptId}/submit`
- Then: HTTP 200 is returned; attempt is persisted with `completedAt` set; `NotificationLog` status is `FAILED` with non-empty `errorMsg`

**AC-N2.2 — No exception reaches the HTTP layer**
- When: Async method throws
- Then: No HTTP 500 is returned; no exception stack trace in the HTTP response body

### US-N3: Notification status tracking

**AC-N3.1 — PENDING status set before processing**
- Given: `SyncTaskExecutor` used in test profile (for deterministic ordering)
- When: Submission is processed
- Then: A `NotificationLog` row with `status = PENDING` is created before `MockEmailService.log()` is called

**AC-N3.2 — SENT status on success**
- When: `MockEmailService` completes without throwing
- Then: `NotificationLog.status = SENT` and `sentAt` is populated

**AC-N3.3 — FAILED status on exception**
- When: `MockEmailService` throws any exception
- Then: `NotificationLog.status = FAILED`; `errorMsg` contains the exception message; `sentAt` is null

**AC-N3.4 — One log row per submission**
- When: `POST /api/attempts/{attemptId}/submit` succeeds
- Then: Exactly 1 `NotificationLog` row exists for `attemptId`; re-submission (HTTP 409) does not create another row

## 8. Out of Scope

- Real email delivery via SMTP, SendGrid, or any external provider — deferred to v2.0.
- Retry logic on notification failure — failures are logged only.
- User opt-out from email notifications.
- Notification for events other than quiz completion (e.g., quiz creation, new quiz available).
- Webhook or push notification alternatives.
- Querying notification status via an API endpoint — status is only in the database.
- Dead-letter queue or alerting on sustained notification failures.
